/*
   Copyright (c) 2015, The Linux Foundation. All rights reserved.
   Copyright (C) 2016 The CyanogenMod Project.
   Copyright (C) 2018-2019 The LineageOS Project
   Copyright (C) 2018-2020 KudProject Development

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
   ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
   OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
   IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <cstdio>

#include <android-base/file.h>
#include <android-base/properties.h>
#include <android-base/strings.h>

#include <sys/sysinfo.h>
#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/_system_properties.h>

#include "vendor_init.h"

using android::base::GetProperty;
using android::base::ReadFileToString;
using android::base::Trim;

void property_override(char const prop[], char const value[])
{
    prop_info *pi;

    pi = (prop_info*) __system_property_find(prop);
    if (pi)
        __system_property_update(pi, value, strlen(value));
    else
        __system_property_add(prop, strlen(prop), value, strlen(value));
}

#define MAX_PROP_LIST 5
void property_override_multiple(std::string type, char const value[])
{
    std::string prop_list[MAX_PROP_LIST] = {
        "ro.product.",
        "ro.product.odm.",
        "ro.product.system.",
        "ro.product.vendor.",
        "ro.vendor.product.",
    };

    for (int i = 0; i < MAX_PROP_LIST; i++)
        property_override((char *)&(prop_list[i] + type)[0], value);
}

void dalvik_properties()
{
    struct sysinfo sys;

    sysinfo(&sys);
    if (sys.totalram > 4096ull * 1024 * 1024) {
        // Set for 6GB or more RAM
        property_override("dalvik.vm.heapstartsize", "16m");
        property_override("dalvik.vm.heapgrowthlimit", "256m");
        property_override("dalvik.vm.heapsize", "512m");
        property_override("dalvik.vm.heaptargetutilization", "0.5");
        property_override("dalvik.vm.heapmaxfree", "32m");
        property_override("dalvik.vm.heapminfree", "8m");
        property_override("ro.vendor.qti.sys.fw.bg_apps_limit", "48");
    } else if (sys.totalram > 3072ull * 1024 * 1024) {
        // Set for 4GB RAM
        property_override("dalvik.vm.heapstartsize", "8m");
        property_override("dalvik.vm.heapgrowthlimit", "192m");
        property_override("dalvik.vm.heapsize", "512m");
        property_override("dalvik.vm.heaptargetutilization", "0.6");
        property_override("dalvik.vm.heapmaxfree", "16m");
        property_override("dalvik.vm.heapminfree", "8m");
        property_override("ro.vendor.qti.sys.fw.bg_apps_limit", "36");
    } else {
        // Set for 2/3GB RAM
        property_override("dalvik.vm.heapstartsize", "8m");
        property_override("dalvik.vm.heapgrowthlimit", "192m");
        property_override("dalvik.vm.heapsize", "512m");
        property_override("dalvik.vm.heaptargetutilization", "0.75");
        property_override("dalvik.vm.heapmaxfree", "8m");
        property_override("dalvik.vm.heapminfree", "512k");
        property_override("ro.vendor.qti.sys.fw.bg_apps_limit", "24");
  }
}

void vendor_check_variant()
{
    struct sysinfo sys;
    char const *region_file = "/mnt/vendor/persist/flag/countrycode.txt";
    char const *product_device, *product_name;
    std::string region;

    sysinfo(&sys);

    // Make sure the region value is trimmed first
    if (ReadFileToString(region_file, &region))
        region = Trim(region);

    if (region == "RU") {
        // Russian model has a slightly different product name
        product_name = "RU_X00TD";
        // Set hardware SKU to enable NFC on Russian model
        property_override("ro.boot.product.hardware.sku", "RU_X00TD");
    } else
        product_name = "WW_X00TD";

    // 6 GB variant
    if (sys.totalram > 4096ull * 1024 * 1024) {
        // Russian model
        if (region == "RU") {
            product_device = "ASUS_X00T_9";

        // Global model
        } else {
            product_device = "ASUS_X00T_3";
        }

    // 3/4 GB variants
    } else {
        // Russian model
        if (region == "RU") {
            product_device = "ASUS_X00T_6";

        // Global model
        } else {
            product_device = "ASUS_X00T_2";
        }
    }

    // Override props based on values set
    property_override_multiple("device", product_device);
    property_override_multiple("name", product_name);
}

void vendor_load_properties()
{
    vendor_check_variant();
    dalvik_properties();
}
