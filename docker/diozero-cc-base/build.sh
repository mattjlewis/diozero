#!/bin/sh

export BUILDAH_LAYERS="false"
podman build --rm -t diozero/diozero-cc-base .
