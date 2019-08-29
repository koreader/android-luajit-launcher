local android = require("android")
local mainLoop = require("main_loop")
local utils = require("utils")

android.log_name = android.getName()
android.notification("hello world!")
utils.sleep(2)
android.notification("press the back button to exit")
mainLoop.run()
