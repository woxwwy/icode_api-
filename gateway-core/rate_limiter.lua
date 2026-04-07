-- 令牌桶限流脚本
-- KEYS[1]: 限流key (如 user:123)
-- ARGV[1]: 桶容量 (如 100)
-- ARGV[2]: 令牌产生速率/秒 (如 10)
-- ARGV[3]: 当前时间戳(毫秒)
-- ARGV[4]: 本次请求需要的令牌数 (通常1)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- 获取当前令牌数和上次更新时间
local bucket = redis.call('HMGET', key, 'tokens', 'last_time')
local tokens = tonumber(bucket[1]) or capacity
local last_time = tonumber(bucket[2]) or now

-- 计算时间差，补充令牌
local delta = math.max(0, now - last_time)
local add_tokens = delta * rate / 1000  -- 转毫秒
tokens = math.min(capacity, tokens + add_tokens)

-- 判断是否足够
if tokens < requested then
    return 0  -- 拒绝
else
    tokens = tokens - requested
    redis.call('HMSET', key, 'tokens', tokens, 'last_time', now)
    redis.call('EXPIRE', key, 60)  -- 60秒过期清理
    return 1  -- 允许
end 