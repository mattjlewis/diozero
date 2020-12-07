---
parent: Internals
nav_order: 1
permalink: /api/startup.html
---

1. Locate the native device factory (command line / env property, ServiceLoader, default)
1. Start the native device factory
1. If using the new GPIO character device, inspect all GPIO chips and lines to auto-populate GPIO information [this should be skipped if there is an existing board definition file]
1. Initialise the board info
1. Detect the local board by inspecting /proc/cpuinfo (fallback to /proc/device-tree/model for Arm64)
1. Instantiate all board info providers (ServiceLoader) to see if they recognise this device
1. Populate pin info using the BoardInfoProvider initialisePins method
    1. This can be in Java code
    1. Or via reading a boarddefs text file; attempt look-up using combinations of values from /proc/device-tree/compatible
1. LibraryLoader will probably get triggered
1. Load libdiozero-system-utils.so via LIBRARY_PATH and CLASSPATH
