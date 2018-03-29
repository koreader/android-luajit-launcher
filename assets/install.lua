local ffi = require("ffi")
local A = require("android")

ffi.cdef[[
struct FILE *fopen(const char *, const char *);
size_t fread(void *, size_t, size_t, struct FILE *);
size_t fwrite(const void *, size_t, size_t, struct FILE *);
int fclose(struct FILE *);
int remove(const char *);

int __cdecl lzma_main(int numargs, char *args[]);
]]

local function install()
    local function check_installed_rev()
        local git_rev = io.open(A.dir .. "/git-rev")
        local rev = git_rev and git_rev:read() or ""
        return rev
    end
    -- 7z compressed package are stored in module directory in asset
    local module = "module"
    local package_name = "koreader%-(.*)%.7z"
    local mgr = A.app.activity.assetManager
    local asset_dir = A.lib.AAssetManager_openDir(mgr, module)
    assert(asset_dir ~= nil, "could not open module directory in assets")
    local filename = A.lib.AAssetDir_getNextFileName(asset_dir)
    while filename ~= nil do
        filename = ffi.string(filename)
        A.LOGI(string.format("Check file in asset %s: %s", module, filename))
        local rev = filename:match(package_name)
        if rev then
            if rev == check_installed_rev() then
                A.LOGI("Skip installation for revision "..rev)
                break
            end
            A.LOGI("Found new package revision "..rev)
            -- copy package from asset
            local package = A.dir.."/"..filename
            local buffer_size = 4096
            local buf = ffi.new("char[?]", buffer_size)
            local asset = A.lib.AAssetManager_open(mgr,
                            ffi.cast("char*", module.."/"..filename),
                            ffi.C.AASSET_MODE_STREAMING);
            if asset ~= nil then
                local output = ffi.C.fopen(ffi.cast("char*", package),
                                ffi.cast("char*", "wb"))
                local nb_read = A.lib.AAsset_read(asset, buf,
                                ffi.new("int", buffer_size))
                while nb_read > 0 do
                    ffi.C.fwrite(buf, ffi.new("int", nb_read),
                                ffi.new("int", 1), output)
                    nb_read = A.lib.AAsset_read(asset, buf,
                                ffi.new("int", buffer_size))
                end
                ffi.C.fclose(output)
                A.lib.AAsset_close(asset)
                -- unpack to data directory
                local args = {"7z", "x", package, A.dir}
                local argv = ffi.new("char*[?]", #args+1)
                for i, arg in ipairs(args) do -- luacheck: ignore 213
                    argv[i-1] = ffi.cast("char*", args[i])
                end
                A.LOGI("Installing new koreader package to "..args[4])
                local lzma = ffi.load("liblzma.so")
                lzma.lzma_main(ffi.new("int", #args), argv)
                ffi.C.remove(ffi.cast("char*", package))
                break
            end
        end
        filename = ffi.string(A.lib.AAssetDir_getNextFileName(asset_dir))
    end
    A.lib.AAssetDir_close(asset_dir)
end

install()
