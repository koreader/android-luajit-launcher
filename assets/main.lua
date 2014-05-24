local ffi = require("ffi")
local A = require("android")

require("test")

local function handle_input(event)
	local ev_type = ffi.C.AInputEvent_getType(event)
	if ev_type == ffi.C.AINPUT_EVENT_TYPE_MOTION then
		local ptr_count = ffi.C.AMotionEvent_getPointerCount(event)
		for i = 0, ptr_count-1 do
			local x = ffi.C.AMotionEvent_getX(event, i)
			local y = ffi.C.AMotionEvent_getY(event, i)
			A.LOGI("motion event at " .. tonumber(x) .. "," .. tonumber(y) .. " for ptr no " .. i)
		end
		return 1
	elseif ev_type == ffi.C.AINPUT_EVENT_TYPE_KEY then
		A.LOGI("got key event, leaving it unhandled")
	end
	return 0
end

local function draw_frame()
	if A.app.window == nil then
		-- No window.
		return
	end

	local buffer = ffi.new("ANativeWindow_Buffer[1]")
	if ffi.C.ANativeWindow_lock(A.app.window, buffer, nil) < 0 then
		A.LOGW("Unable to lock window buffer");
		return
	end

	local bb = nil
	if buffer[0].format == ffi.C.WINDOW_FORMAT_RGBA_8888
	or buffer[0].format == ffi.C.WINDOW_FORMAT_RGBX_8888
	then
		-- modify buffer[0].bits here
	elseif buffer[0].format == ffi.C.WINDOW_FORMAT_RGB_565 then
		-- modify buffer[0].bits here
	else
		A.LOGE("unsupported window format!")
	end

	ffi.C.ANativeWindow_unlockAndPost(A.app.window);
end

local function handle_cmd(cmd)
	A.LOGI("got command: " .. tonumber(cmd))
	if cmd == ffi.C.APP_CMD_INIT_WINDOW then
		draw_frame()
	elseif cmd == ffi.C.APP_CMD_TERM_WINDOW then
		-- do nothing for now
	elseif cmd == ffi.C.APP_CMD_LOST_FOCUS then
		draw_frame()
	end
end

local function loop()
	local events = ffi.new("int[1]")
	local source = ffi.new("struct android_poll_source*[1]")
	while true do
		-- see the Android NDK documentation (the header files, that is!) in order
		-- to get an idea how to use ALooper to poll your filedescriptors, too.
		if ffi.C.ALooper_pollAll(-1, nil, events, ffi.cast("void**", source)) >= 0 then
			-- we do the event handling here on our own since we do not want
			-- to create unnecessary C callbacks
			if source[0] ~= nil then
				if source[0].id == ffi.C.LOOPER_ID_MAIN then
					local cmd = ffi.C.android_app_read_cmd(A.app)
					ffi.C.android_app_pre_exec_cmd(A.app, cmd)
					handle_cmd(cmd)
					ffi.C.android_app_post_exec_cmd(A.app, cmd)
				elseif source[0].id == ffi.C.LOOPER_ID_INPUT then
					local event = ffi.new("AInputEvent*[1]")
					while ffi.C.AInputQueue_getEvent(A.app.inputQueue, event) >= 0 do
						if ffi.C.AInputQueue_preDispatchEvent(A.app.inputQueue, event[0]) == 0 then
							ffi.C.AInputQueue_finishEvent(A.app.inputQueue, event[0],
								handle_input(event[0]))
						end
					end
				end
			end
			if A.app.destroyRequested ~= 0 then
				A.LOGI("Engine thread destroy requested!")
				return
			end
		end
	end
end

loop()
