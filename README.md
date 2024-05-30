# Traffic-monitoring-based-on-eBPF-on-Android-platform
This is a practice project, and the author might only industrialize it when the need arises much later.

This is a traffic monitoring tool based on eBPF technology for the Android platform, featuring a simple Android graphical interface. The eBPF part is implemented based on eunomia-bpf.

The main principles are:

	1.	Use eBPF to intercept Ethernet frames from the link layer in the kernel.
	2.	Perform partial protocol parsing and then send the data to user space for further protocol parsing and information classification.
	3.	Store the classified results in a database.
