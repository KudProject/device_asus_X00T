/*
 * Copyright (C) 2014, 2017-2018 The  Linux Foundation. All rights reserved.
 * Not a contribution
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2018-2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "LightsService"

#include "Light.h"
#include <android-base/logging.h>
#include <fstream>

namespace {
/*
 * Write value to path and close file.
 */
template <typename T>
static void set(const std::string& path, const T& value) {
    std::ofstream file(path);

    if (!file.is_open()) {
        LOG(DEBUG) << "failed to open path " << path;
        return;
    }

    file << value;

    if (file.fail()) {
        LOG(DEBUG) << "failed to write " << value
                   << "to " << path;
    }
}

/*
 * Read from path and close file.
 * Return def in case of any failure.
 */
template <typename T>
static T get(const std::string& path, const T& def) {
    std::ifstream file(path);
    T result;

    if (!file.is_open()) {
        LOG(DEBUG) << "failed to open path " << path;
        return def;
    }

    file >> result;

    if (file.fail()) {
        LOG(DEBUG) << "failed to read from " << path;
        return def;
    }

    return result;
}
}  // anonymous namespace

namespace android {
namespace hardware {
namespace light {
namespace V2_0 {
namespace implementation {

static constexpr int kDefaultMaxBrightness = 255;
static constexpr char kBlink[] = "blink";
static constexpr char kBrightness[] = "brightness";
static constexpr char kDelayOff[] = "pause_lo";
static constexpr char kDelayOn[] = "pause_hi";

static uint32_t rgbToBrightness(const LightState& state) {
    uint32_t color = state.color & 0x00ffffff;
    return ((77 * ((color >> 16) & 0xff))
            + (150 * ((color >> 8) & 0xff))
            + (29 * (color & 0xff))) >> 8;
}

Light::Light() {
    mLights.emplace(Type::ATTENTION, std::bind(&Light::handleNotification, this, std::placeholders::_1, 0));
    mLights.emplace(Type::BACKLIGHT, std::bind(&Light::handleBacklight, this, std::placeholders::_1));
    mLights.emplace(Type::BATTERY, std::bind(&Light::handleNotification, this, std::placeholders::_1, 1));
    mLights.emplace(Type::NOTIFICATIONS, std::bind(&Light::handleNotification, this, std::placeholders::_1, 2));
}

void Light::handleBacklight(const LightState& state) {
    uint32_t maxBrightness = get("/sys/class/leds/lcd-backlight/max_brightness", kDefaultMaxBrightness);
    uint32_t sentBrightness = rgbToBrightness(state);
    uint32_t brightness = sentBrightness * maxBrightness / kDefaultMaxBrightness;
    LOG(DEBUG) << "Writing backlight brightness " << brightness
               << " (orig " << sentBrightness << ")";
    set("/sys/class/leds/lcd-backlight/brightness", brightness);
}

void Light::handleNotification(const LightState& state, size_t index) {
    mLightStates.at(index) = state;

    LightState stateToUse = mLightStates.front();
    for (const auto& lightState : mLightStates) {
        if (lightState.color & 0xffffff) {
            stateToUse = lightState;
            break;
        }
    }

    // Extract brightness from AARRGG
    uint32_t alpha = (stateToUse.color >> 24) & 0xff;

    std::map<std::string, int> colorValues;
    colorValues["red"] = (stateToUse.color >> 16) & 0xff;
    colorValues["green"] = (stateToUse.color >> 8) & 0xff;

    // Scale RG colors if a brightness has been applied by the user
    if (alpha != 0xff) {
        for (auto& entry : colorValues) {
            // For more exact scaling divide by half
            entry.second = ((entry.second * alpha) / 0xff) / 2;
        }
    }

    auto makeLedPath = [](const std::string& led, const char op[]) -> std::string {
        return "/sys/class/leds/" + led + "/" + op;
    };

    // Disable all blinking before starting
    for (const auto& entry : colorValues) {
        set(makeLedPath(entry.first, kBlink), 0);
    }

    if (state.flashMode == Flash::TIMED) {
        for (const auto& entry : colorValues) {
            set(makeLedPath(entry.first, kDelayOff), state.flashOffMs);
            set(makeLedPath(entry.first, kDelayOn), state.flashOnMs);
        }

        // Start blinking
        for (const auto& entry : colorValues) {
            set(makeLedPath(entry.first, kBlink), entry.second);
        }
    } else {
        for (const auto& entry : colorValues) {
            set(makeLedPath(entry.first, kBrightness), entry.second);
        }
    }
}

Return<Status> Light::setLight(Type type, const LightState& state) {
    auto it = mLights.find(type);

    if (it == mLights.end()) {
        return Status::LIGHT_NOT_SUPPORTED;
    }

    // Lock global mutex until light state is updated.
    std::lock_guard<std::mutex> lock(mLock);

    it->second(state);

    return Status::SUCCESS;
}

Return<void> Light::getSupportedTypes(getSupportedTypes_cb _hidl_cb) {
    std::vector<Type> types;

    for (auto const& light : mLights) {
        types.push_back(light.first);
    }

    _hidl_cb(types);

    return Void();
}

}  // namespace implementation
}  // namespace V2_0
}  // namespace light
}  // namespace hardware
}  // namespace android
