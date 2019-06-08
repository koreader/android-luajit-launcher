#!/bin/bash

BASE_PATH="../src/org/koreader/device"
TARGET_PATH="src/org/koreader/test/eink"

# Rockchip EPD Controllers
ROCKCHIP_PATH="${BASE_PATH}/rockchip"
ROCKCHIP_EPD="RK30xxEPDController RK33xxEPDController"

# Freescale EPD Controllers
FREESCALE_PATH="${BASE_PATH}/freescale"
FREESCALE_EPD="NTXEPDController"

for EPD in ${ROCKCHIP_EPD}; do
    src="${ROCKCHIP_PATH}/${EPD}.java"
    dest="${TARGET_PATH}/${EPD}.java"
    echo "copying ${EPD} controller from ${src} to ${dest}"
    sed 's/package org.koreader.device.rockchip/package org.koreader.test.eink/' "${src}" >"${dest}"
done

for EPD in ${FREESCALE_EPD}; do
    src="${FREESCALE_PATH}/${EPD}.java"
    dest="${TARGET_PATH}/${EPD}.java"
    echo "copying ${EPD} controller from ${src} to ${dest}"
    sed 's/package org.koreader.device.freescale/package org.koreader.test.eink/' "${src}" >"${dest}"
done
