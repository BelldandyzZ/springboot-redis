# 一、什么是缓存

> 缓存就是数据交换的缓冲区，是存储数据的临时空间，一般读写性能高

![](img/缓存的优缺点.jpg)



# 二、添加缓存的流程

![](img/redis缓存作用模型.jpg)



# 三、缓存更新策略

![](img/缓存更新策略.jpg)

## ① 主动更新策略的三种方式

![](img/主动更新策略.jpg)

❓❓数据库数据变更时操作缓存有三个问题需要考虑



1. 数据库变更时是删除缓存还是更新缓存
   - 更新缓存：数据库更新100次，缓存也更新100次，如果这个期间没有人来访问缓存，那这个更新是无意义的。
   - **删除缓存：不管数据库更新多少次，都是删除缓存，等有人访问时才从数据库查询数据添加缓存，类似懒加载**
2. 数据库变更时操作缓存如何保证数据库操作与缓存操作同时成功或同时失败
   - 单体系统：数据库操作与缓存操作放在一个事务
   - 分布式系统：利用TCC等分布式事务解决方案

3. 先操作缓存还是先操作数据库

   - **先操作数据库，再操作缓存**比先操作缓存再操作数据库发生线程安全问题的可能性低

     ![](img/数据库与缓存操作的顺序.jpg)

## ②缓存更新策略的最佳方案

- 低一致性需求：使用redis的内存淘汰方案

- **高一致性需求：主动更新，并使用超时剔除作为兜底**

  > 读操作：缓存命中直接返回。未命中则查询数据库，写入缓存并设置超时时间。

  > 写操作：先写数据库再删除缓存，要确保数据库与缓存操作的原子性。



# 三、缓存穿透

> 缓存穿透是指客户端的请求在缓存和数据库中都不存在，这样缓存永远都不会生效。请求直接打到数据库



![](img/缓存穿透的解决方案.jpg)

**缓存空对象解决缓存穿透**

![](img/Snipaste_2023-04-24_16-30-35.jpg)



# 四、缓存雪崩

> 缓存雪崩是指在同一时段内大量的缓存key同时失效或Redis宕机，导致大量请求直达数据库

![](img/缓存雪崩.jpg)

# 五、缓存击穿

> 缓存击穿问题也叫热点key问题，就是一个被高并发访问，并且缓存重建业务比较复杂的key突然失效了，导致大量请求直达数据库

![](img/缓存雪崩.jpg)

![](img/缓存击穿的两种解决方案.jpg)

![](img/缓存击穿的两种解决方案优缺点.jpg)

> 互斥锁保证的是数据一致性问题，不保证性能。逻辑过期性能较好，但是不保证数据一致性 

## ①基于互斥锁解决缓存击穿

> 代码见git`美食列表中的商户查询-缓存击穿之互斥锁解决方案version2`

## ②基于逻辑过期解决缓存击穿

![](img/基于逻辑过期解决缓存击穿.jpg)

> 代码见git`美食列表中的商户查询-缓存击穿之逻辑过期解决方案`