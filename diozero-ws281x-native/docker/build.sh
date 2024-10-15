#!/bin/sh

BUILDAH_LAYERS="false"
podman build --rm -t diozero/diozero-ws281x-cc .
