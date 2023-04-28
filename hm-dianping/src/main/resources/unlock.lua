local cacheThreadId = redis.call("get",KEYS[1])
local curThreadId = ARGV[1]

if(curThreadId == cacheThreadId)
then
    redis.call("del",KEYS[1])
end
return 0
