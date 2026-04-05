@Component
public class SlidingWindow implements RateLimiter {
    // 每个ip存一个请求时间队列
    private Map<String, Queue<Long>> ipWindows = new HashMap<>();
    
    @Value("${ratelimit.limit:100}")
    private int limit;
    
    @Value("${ratelimit.windowMs:1000}")
    private long windowMs;
    
    @Override
    public boolean allow(String ip) {
        // 防止空指针，给个默认ip
        if (ip == null || ip.equals("")) {
            ip = "0.0.0.0";
        }
        
        long now = System.currentTimeMillis();
        
        // 对整个map加锁
        synchronized (ipWindows) {
            // 拿到这个ip的队列，if没有我呢就新建
            Queue<Long> timeList = ipWindows.get(ip);
            if (timeList == null) {
                timeList = new LinkedList<>();
                ipWindows.put(ip, timeList);
            }
            
            // 把超时的旧记录清掉
            while (!timeList.isEmpty()) {
                Long oldTime = timeList.peek();
                if (now - oldTime > windowMs) {
                    timeList.poll();  // 扔掉过期的
                } else {
                    break;  // 遇到没过期的，后面的都不用看了
                }
            }
            
            // 看看窗口里还剩多少请求
            if (timeList.size() < limit) {
                timeList.add(now);  // 把现在这条记进去
                return true;        // 放行
            } else {
                return false;       // 窗口满了，拒绝
            }
        }
    }
<<<<<<< HEAD
}
=======
>>>>>>> 257bd779e10e3f7c9c5c85a8be2d6efeff98dd63
