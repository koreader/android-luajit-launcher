#!/bin/bash

BASE_PATH="../src/org/koreader/device"
TARGET_PATH="src/org/koreader/test/eink"

# Rockchip EPD Controllers
ROCKCHIP_PATH="${BASE_PATH}/rockchip"
ROCKCHIP_EPD="RK30xxEPDController RK33xxEPDController"

for EPD in ${ROCKCHIP_EPD}; do
    src="${ROCKCHIP_PATH}/${EPD}.java"
    dest="${TARGET_PATH}/${EPD}.java"
    echo "copying ${EPD} controller from ${src} to ${dest}"
    sed 's/package org.koreader.device.rockchip/package org.koreader.test.eink/' "${src}" >"${dest}"
done
