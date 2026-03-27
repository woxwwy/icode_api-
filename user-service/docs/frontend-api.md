# 前端接口文档

## 获取路由列表

### 请求信息
- **URL**: `/api/routes`
- **方法**: `GET`
- **返回格式**: JSON

### 返回数据格式
```json
[
  {
    "id": "string",
    "path": "string",
    "uri": "string",
    "component": "string",
    "name": "string",
    "status": "string"
  }
]