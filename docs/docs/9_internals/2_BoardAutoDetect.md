---
parent: Internals
nav_order: 2
permalink: /internals/boardautodetect.html
---
# Board Auto-detection

* Default behaviour in BaseNativeDeviceFactory is to inspect the local device and auto-discover capabilities
    * `lookupBoardInfo(): BoardInfo (extends BoardPinInfo)`
* `BoardInfoProvider#lookup(LocalSystemInfo)` method for detecting a new board at runtime based on LocalSystemInfo
* `ServiceLoader` implementation that loads all declared board info providers on startup (from classpath) - `LocalBoardInfoUtil.resolveLocalBoardInfo`
* `BoardInfo` class responsible for defining the board's physical connectivity - returned from the `BoardInfoProvider` lookup method
    * Allows full control of the board layout from within Java code, example `PiBRev1BoardInfo`
* `GenericLinuxArmBoardInfo` for loading the board definition file by reading `/proc/device-tree/compatible`
    * Preferred approach for Linux SBCs - extend GenericLinuxArmBoardInfo which loads layout information from
    a simple text file
    * Format of the file is multiple values separated by NULL character (\0), each value optionally comma
    separated into multiple parts. Typically `<<Board vendor>>, <<Board model>>` + `<<CPU vendor>>, <<CPU model>>`
    * Attempt to resolve by combining parts, give example, e.g. `hardkernel,odroid-c2^@amlogic,meson-gxbb^@` &rarr;
    `hardkernel_ordriod-c2`, `hardkernel`, `amlogic_meson-gxbb`, `amlogic`
    * Location and format of the board def file
    * The Raspberry Pi `PiBoardInfo` class extends `GenericLinuxArmBoardInfo` and overrides populateBoardPinInfo so
    that it can be reused by the remote PigpioJ provider
* If this fails to correctly identify the board and populate physical connectivity discovery will be
attempted via GPIO character device names. This will only work if the GPIOs are named correctly,
i.e. GPIO1, GPIO2, etc, which is often not the case.
* The `BaseNativeDeviceFactory.lookupBoardInfo()` method can be overridden as is the case for the following providers:
    * Firmata
    * PigpioJ
    * Remote
    * Mock
* With the exception of the Mock provider, all of these support connecting to a remotely attached
device, hence cannot simply inspect the locally attached device. For example, you could be running
on a Windows desktop from within an IDE connecting to a micro-controller over a serial connection.

## Adding Support for a new Linux ARM SBC

TBD
