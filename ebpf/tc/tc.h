#ifndef __BOOTSTRAP_H
#define __BOOTSTRAP_H

#define MAX_BUF_SIZE 	64
#define ETH_ALEN	6


struct so_event {
	unsigned char	h_dest[ETH_ALEN];	/* destination eth addr	*/
	unsigned char	h_source[ETH_ALEN];	/* source ether addr	*/
	__be16		h_proto;
	
	__u8	version;
	__u8  	ihl;
	__u16	tot_len;
	__u16	id;
	__u8    DF;
	__u8    MF;
	__u16	frag_off;
	__u8	ttl;
	__u8	protocol;
	__be32  saddr;
	__be32  daddr;
	
	__u64	event_id; 
	__u16  	event_frag_index;	
        unsigned char	payload[MAX_BUF_SIZE];
};

#endif
