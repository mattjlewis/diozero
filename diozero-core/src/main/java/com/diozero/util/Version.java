package com.diozero.util;

public class Version {
	private int major;
	private int minor;
	private int point;

	public Version(int major, int minor, int point) {
		this.major = major;
		this.minor = minor;
		this.point = point;
	}

	public Version(String s) {
		final String[] parts = s.split("\\.");
		major = Integer.parseInt(parts[0]);
		minor = Integer.parseInt(parts[1]);
		point = Integer.parseInt(parts[2].split("-")[0]);
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPoint() {
		return point;
	}

	@Override
	public String toString() {
		return "Version [" + major + "." + minor + "." + point + "]";
	}

	public static void main(String[] args) {
		// Self test
		System.out.println(new Version("2.6.18-92.el5"));

		String version = "Linux version 6.1.21-v7l+ (dom@buildbot) (arm-linux-gnueabihf-gcc-8 (Ubuntu/Linaro 8.4.0-3ubuntu1) 8.4.0, GNU ld (GNU Binutils for Ubuntu) 2.34) #1642 SMP Mon Apr  3 17:22:30 BST 2023";
		String version_string = version.replaceAll("^Linux version (\\d+\\.\\d+\\.\\d+(?:-.+?)?) .*$", "$1");
		System.out.println(new Version(version_string));
	}
}
