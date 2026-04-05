// src/mock/mockroutes.js
// 这是一个模拟的后端返回数据，用于前端开发阶段展示

// 需要和后端（伟琛）确认最终接口返回的字段名，保持一致。


//-------------3.21新增（获取后端真实数据）---------）
// 获取路由数据的函数（从后端获取）
export async function getRoutes() {
  try {
    const response = await fetch('http://localhost:8081/api/routes'); //应该是8081调用网关
    const data = await response.json();
    return data;
  } catch (error) {
    console.error('获取路由数据失败:', error);
    return [];
  }
}
//----------新增结束-------------


export const mockRoutes = [
  {
    id: 'user-service-route',          //路由的唯一标识（开发者起的名字）
    path: '/api/user/**',              //转发规则，匹配以/api/user/开头的请求
    uri:'http://localhost:8082',           //转发目标地址（后端服务地址）
    component: 'user-service',        //对应的后端服务/组件（一个标识）         // 或者别的含义，按项目实际来
    name: '用户服务路由',              //用户友好型路由名称
    status: 'normal'                 // 可选，用于状态列  (在我原本的routes文件中，写死了状态为正常，后续可以加入ref响应变量来改)            
  },
  {
    id: 'order-service-route',
    path: '/api/order/**',
    uri:'http://localhost:8083',           //转发目标地址（后端服务地址）
    component: 'order-service',
    name: '订单服务路由',
    status: 'normal' 
  },
  {
    id: 'product-service-route',
    path: '/api/product/**',
    uri:'http://localhost:8084',           //转发目标地址（后端服务地址）
    component: 'product-service',
    name: '商品服务路由',
    status: 'normal' 
  }
];
