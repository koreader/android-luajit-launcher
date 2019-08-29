local ffi = require("ffi")
local C = ffi.C

local android = require("android")
local utils = require("utils")
----------------------------------

local function finish()
    -- finish the native activity. On destroy will be called and our
    -- input queue dettached. lua/JNI will become unavailable.
    android.lib.ANativeActivity_finish(android.app.activity)

    -- let the VM dies before us.
    utils.sleep(1)
    android.LOGW("bye...")
    os.exit()
end

local function processState(callback)
    if callback == C.APP_CMD_RESUME then
        android.LOGI("onResume callback")
    elseif callback == C.APP_CMD_PAUSE then
        android.LOGI("onPause callback")
    end
end

local function processEvent(event)
    -- consume all events here to avoid ANRs in this example app.
    -- Full blown apps will want to forward some events to the system
    -- for further proccesing, ie: media and volume keys.
    android.lib.AInputQueue_finishEvent(android.app.inputQueue, event, 1)

    -- we take care of key and motion events
    local event_type = android.lib.AInputEvent_getType(event)

    if event_type == C.AINPUT_EVENT_TYPE_KEY then
        local code = android.lib.AKeyEvent_getKeyCode(event)
        local action = android.lib.AKeyEvent_getAction(event)
        if code == C.AKEYCODE_BACK then
            -- finish the program with the back key.
            android.notification("bye!")
            utils.sleep(1)
            finish()
        elseif action == C.AKEY_EVENT_ACTION_DOWN then
            -- show a "pressed" notification with key code
            android.notification("Pressed key "..code)
        end
    elseif event_type == C.AINPUT_EVENT_TYPE_MOTION then
        -- DEBUG statements will be skipped on release builds
        android.DEBUG("yet another motion event detected...")
    end
end

local mainLoop = {}

function mainLoop.run()
    -- main loop. Android will show an "Application Not Responding" (ANR) 
    -- dialog if it finds an event queued for more than 5 seconds.
    while true do
        local events = ffi.new("int[1]")
        local source = ffi.new("struct android_poll_source*[1]")
        local poll_state = android.lib.ALooper_pollAll(-1, nil, events, ffi.cast("void**", source))
        if poll_state >= 0 then
            if source[0] ~= nil then
                if source[0].id == C.LOOPER_ID_MAIN then
                    local cmd = C.android_app_read_cmd(android.app)
                    C.android_app_pre_exec_cmd(android.app, cmd)
                    processState(cmd)
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
end

return mainLoop
