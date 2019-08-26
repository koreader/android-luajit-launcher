--[[ example application using android-luajit-launcher ]]--
local ffi = require("ffi")
local C = ffi.C

ffi.cdef[[
    unsigned int sleep(unsigned int seconds);
]]

local android = require("android")
----------------------------------

local function init()
    -- set the logger name as the app name
    android.log_name = android.getName()
end

local function processEvent(event)
    local event_type = android.lib.AInputEvent_getType(event)
    -- consume all events here to avoid ANRs
    android.lib.AInputQueue_finishEvent(android.app.inputQueue, event, 1)
    if event_type == C.AINPUT_EVENT_TYPE_KEY then
        local key_code = android.lib.AKeyEvent_getKeyCode(event)
        if key_code == C.AKEYCODE_BACK then
            android.notification("bye!")
            ffi.C.sleep(1)
            android.lib.ANativeActivity_finish(android.app.activity)
        else
            android.notification("Pressed key "..key_code)
        end
    elseif event_type == C.AINPUT_EVENT_TYPE_MOTION then
        android.LOGI("motion event detected")
    end
end

------ program begins ------------
init()
android.notification("hello world")
ffi.C.sleep(3)
android.notification("press the back button to exit")
while true do
    local events = ffi.new("int[1]")
    local source = ffi.new("struct android_poll_source*[1]")
    local poll_state = android.lib.ALooper_pollAll(-1, nil, events, ffi.cast("void**", source))
    if poll_state >= 0 then
        if source[0] ~= nil then
            if source[0].id == C.LOOPER_ID_MAIN then
                local cmd = C.android_app_read_cmd(android.app)
                C.android_app_pre_exec_cmd(android.app, cmd)
                C.android_app_post_exec_cmd(android.app, cmd)
            elseif source[0].id == C.LOOPER_ID_INPUT then
                local event = ffi.new("AInputEvent*[1]")
                while android.lib.AInputQueue_getEvent(android.app.inputQueue, event) >= 0 do
                    if android.lib.AInputQueue_preDispatchEvent(android.app.inputQueue, event[0]) == 0 then
                        processEvent(event[0])
                    end
                end
            end
            if android.app.destroyRequested ~= 0 then
                android.LOGI("Engine thread destroy requested!")
                return
            end
        elseif poll_state == C.ALOOPER_POLL_TIMEOUT then
            error("Waiting for input failed: timeout\n")
        end
    end
end
