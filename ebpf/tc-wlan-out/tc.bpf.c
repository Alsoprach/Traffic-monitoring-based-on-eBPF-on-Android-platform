#include <vmlinux.h>
#include <bpf/bpf_endian.h>
#include <bpf/bpf_helpers.h>
#include <bpf/bpf_tracing.h>

#include "tc.h"

#define TC_ACT_OK 0

#define ETH_P_IP 0x0800 /* Internet Protocol packet */
#define IP_P_TCP 0x6

#define ethhdr_len   (sizeof(struct ethhdr)) 
#define iphdr_len    (sizeof(struct iphdr ))

#define ethhdr_off 0
#define iphdr_off  ( ethhdr_len + ethhdr_off)

#define str_cpy(x,y) for(int i=0;i<ETH_ALEN;i++)x[i]=y[i];
#define struct_cpy(x,y) str_cpy(x->h_dest,y->h_dest);\
			str_cpy(x->h_source,y->h_source);\
			x->h_proto 	= y->h_proto;\
			x->version 	= y->version;\
			x->ihl 		= y->ihl;\
			x->tot_len 	= y->tot_len;\
			x->id 		= y->id;\
			x->DF 		= y->MF;\
			x->MF 		= y->MF;\
			x->frag_off 	= y->frag_off;\
			x->ttl 		= y->ttl;\
			x->protocol 	= y->protocol;\
			x->saddr 	= y->saddr;\
			x->daddr	= y->daddr;

#define min(x,y) (x>y)?y:x
#define max(x,y) (x>y)?x:y

struct {
 __uint(type, BPF_MAP_TYPE_HASH);
 __uint(max_entries, 16);
 __type(key, __u32);
 __type(value, __u64);
} values SEC(".maps");

struct {
    __uint(type, BPF_MAP_TYPE_RINGBUF);
    __uint(max_entries, 256 * 1024);
} rb SEC(".maps");	

/// @tchook {"ifindex":16, "attach_point":"BPF_TC_EGRESS"}
/// @tcopts {"handle":1, "priority":1}
SEC("tc")
int tc_ingress(struct __sk_buff *ctx)
{
    struct so_event *e;
    e = bpf_ringbuf_reserve(&rb, sizeof(*e), 0);
    if (!e)
        return 0;
    
    // Ethernet protocol analysis --------------------
    // Temp Eth Proto Var
    __be16 proto;
    
    // Eth Data Get
    bpf_skb_load_bytes(ctx, ethhdr_off + offsetof(struct ethhdr, h_dest)  , &e->h_dest  , sizeof(ETH_ALEN));
    bpf_skb_load_bytes(ctx, ethhdr_off + offsetof(struct ethhdr, h_source), &e->h_source, sizeof(ETH_ALEN));
    
    // Into Event
    e->h_proto 	= bpf_ntohs(ctx->protocol);

    // Check Proto
    if (ctx->protocol != bpf_ntohs(ETH_P_IP)){
    	bpf_ringbuf_discard(e, 0);
	return 0;
    }

    // IP protocol analysis --------------------------
    // Temp IP Proto Var
    __u8   version_ihl;
    __u16 tot_len;
    __u16 id;
    __u16 flag;
    __u8  ttl;
    __u8  ip_proto;
    __u32 saddr;
    __u32 daddr;
    // IP Data Get
    bpf_skb_load_bytes(ctx, iphdr_off 					, &version_ihl	, sizeof(__u8 ));
    bpf_skb_load_bytes(ctx, iphdr_off + offsetof(struct iphdr, tot_len) , &tot_len	, sizeof(__u16));
    bpf_skb_load_bytes(ctx, iphdr_off + offsetof(struct iphdr, id)      , &id	  	, sizeof(__u16));
    bpf_skb_load_bytes(ctx, iphdr_off + offsetof(struct iphdr, frag_off), &flag   	, sizeof(__u16));
    bpf_skb_load_bytes(ctx, iphdr_off + offsetof(struct iphdr, ttl)	, &ttl	        , sizeof(__u8 ));
    bpf_skb_load_bytes(ctx, iphdr_off + offsetof(struct iphdr, protocol), &ip_proto     , sizeof(__u8 ));
    bpf_skb_load_bytes(ctx, iphdr_off + offsetof(struct iphdr, saddr)	, &saddr	, sizeof(__u32));
    bpf_skb_load_bytes(ctx, iphdr_off + offsetof(struct iphdr, daddr)	, &daddr	, sizeof(__u32));
    // Into Event
    e->version	= (version_ihl & 0xf0) >> 4;
    e->ihl 	=  version_ihl & 0xf ;   
    e->tot_len  =  bpf_ntohs(tot_len);
    e->id       =  bpf_ntohs(id);
    e->DF     	=  ( bpf_ntohs(flag) >> 14) & 1; 
    e->MF	=  ( bpf_ntohs(flag) >> 13) & 1;
    e->frag_off	=  ( bpf_ntohs(flag) )      & 0x1fff;
    e->ttl	=  ttl;
    e->protocol =  ip_proto;
    e->saddr	=  __bpf_ntohl(saddr);
    e->daddr	=  __bpf_ntohl(daddr);
    // Check Proto
    if ( e->protocol != IP_P_TCP){
    	bpf_ringbuf_discard(e, 0);
        return 0;
    }
    

    // Custom event submit protocol ---------------------------
    __u16	event_frag_index 	= 1;
    __u64	event_id 		= 1; 
    __u64 	*event_id_p 		= NULL;
  
    __u32	key = 1;    
    
    __u16 re_len        = e->tot_len - e->ihl * 4 ;
    __u32 ip_proto_off  = iphdr_off  + e->ihl * 4 ;
	
    // HTTP Check
    char line_buffer[7];
    __u8 doff;
    bpf_skb_load_bytes(ctx, ip_proto_off + offsetof(struct tcphdr, ack_seq)+4 , &doff, sizeof(doff));
    doff &= 0xf0; // clean-up res1
    doff >>= 4; // move the upper 4 bits to low
    doff *= 4; // convert to bytes length

    bpf_skb_load_bytes(ctx, ip_proto_off + doff  , line_buffer  , 7 );
    if (bpf_strncmp(line_buffer, 3, "GET") != 0 &&
        bpf_strncmp(line_buffer, 4, "POST") != 0 &&
        bpf_strncmp(line_buffer, 3, "PUT") != 0 &&
        bpf_strncmp(line_buffer, 6, "DELETE") != 0 &&
        bpf_strncmp(line_buffer, 4, "HTTP") != 0)
    {
            bpf_ringbuf_discard(e, 0);
	    return 0;
    }

    if(re_len < 64){
        __u16 i = 0;
      
        if( re_len & 1<<0 ){
        	bpf_skb_load_bytes(ctx, ip_proto_off + i , &(e->payload[i])  , 1 );
                i += 1;
        }
        if( re_len & 1<<1 ){
                bpf_skb_load_bytes(ctx, ip_proto_off + i , &(e->payload[i])  , 2 );
                i += 1<<1;
        }
        if( re_len & 1<<2 ){
        	bpf_skb_load_bytes(ctx, ip_proto_off + i , &(e->payload[i])  , 4 );
        	i += 1<<2;
        }
        if( re_len & 1<<3 ){
                bpf_skb_load_bytes(ctx, ip_proto_off + i , &(e->payload[i])  , 8 );
                i += 1<<3;
        }
        if( re_len & 1<<4 ){
           	bpf_skb_load_bytes(ctx, ip_proto_off + i , &(e->payload[i])  , 16 );
                i += 1<<4;
        }
        if( re_len & 1<<5 ){
                bpf_skb_load_bytes(ctx, ip_proto_off + i , &(e->payload[i])  , 32 );
                i += 1<<5;
        }
	
	e->event_id = 0;
	e->event_frag_index = 0;
	bpf_ringbuf_submit(e, 0);
	return TC_ACT_OK;


    }
    else {
	event_id_p= bpf_map_lookup_elem(&values, &key);
    	if ( event_id_p == NULL ){
    	  bpf_map_update_elem(&values, &key, &event_id, BPF_ANY);
    	}
    	else {
    	  event_id = *event_id_p + 1;
    	  bpf_map_update_elem(&values, &key, &event_id, BPF_ANY);
    	}
	
	__u32 off = 0;
	for( int u=1024 ; u  > 0 ; u -=1){
   		struct so_event *tmp_e;
    		tmp_e = bpf_ringbuf_reserve(&rb, sizeof(*tmp_e), 0);
        	if(!tmp_e){
		 	bpf_ringbuf_discard(e, 0);
		 	return 0;
		} 
    		struct_cpy(tmp_e,e); 
		
		 bpf_skb_load_bytes(ctx, ip_proto_off + off , &(tmp_e->payload)  , 64 );

		tmp_e->event_id = event_id;
		tmp_e->event_frag_index = event_frag_index;
		event_frag_index += 1;
		off+=64;
		bpf_ringbuf_submit(tmp_e, 0);
		if(re_len-off < 64)break;
	}

	if(re_len-off>0){
		struct so_event *tmp_e;
                tmp_e = bpf_ringbuf_reserve(&rb, sizeof(*tmp_e), 0);
                if(!tmp_e){
                        bpf_ringbuf_discard(e, 0);
                        return 0;
                }
                struct_cpy(tmp_e,e);

		__u16 ri = 0;
		re_len = re_len - off;
        	if( re_len & 1<<0 ){
                	bpf_skb_load_bytes(ctx, ip_proto_off + off + ri , &(tmp_e->payload[ri])  , 1 );
                	ri += 1;
        	}
        	if( re_len & 1<<1 ){
                	bpf_skb_load_bytes(ctx, ip_proto_off + off + ri , &(tmp_e->payload[ri])  , 2 );
                	ri += 1<<1;
        	}
        	if( re_len & 1<<2 ){
                	bpf_skb_load_bytes(ctx, ip_proto_off + off + ri , &(tmp_e->payload[ri])  , 4 );
                	ri += 1<<2;
        	}
        	if( re_len & 1<<3 ){
                	bpf_skb_load_bytes(ctx, ip_proto_off + off + ri , &(tmp_e->payload[ri])  , 8 );
                	ri += 1<<3;
       		}	
        	if( re_len & 1<<4 ){
                	bpf_skb_load_bytes(ctx, ip_proto_off + off + ri , &(tmp_e->payload[ri])  , 16 );
                	ri += 1<<4;
        	}	
        	if( re_len & 1<<5 ){
                	bpf_skb_load_bytes(ctx, ip_proto_off + off + ri , &(tmp_e->payload[ri])  , 32 );
                	ri += 1<<5;
        	}

		tmp_e->event_id = event_id;
                tmp_e->event_frag_index = event_frag_index;
                
                bpf_ringbuf_submit(tmp_e, 0);
	}

	bpf_ringbuf_discard(e, 0);
        return TC_ACT_OK;
    }
    
 
        

      
    

    

    	
        

    

    //struct so_event *tmp_e;
    //tmp_e = bpf_ringbuf_reserve(&rb, sizeof(*tmp_e), 0);
    //if (!tmp_e)return 0;
    //struct_cpy(tmp_e,e);
	
    
    //bpf_ringbuf_discard(tmp_e, 0);
    return TC_ACT_OK;
}

char __license[] SEC("license") = "GPL";
