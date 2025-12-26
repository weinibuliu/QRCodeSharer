# QRCode Sharer

QRCode Sharer 允许你解析二维码后直接上传 URL 而不是图像至服务器。

## 平台支持
- Android
- HarmonyOS
- HarmonyOS NEXT(需要使用卓易通)

## 功能特性

- 扫描二维码并自动上传解析结果到服务器
- 订阅其他用户，实时同步并生成二维码
- 根据平台自适应样式的 Native UI
- 响应式布局，支持常见智能手机与平板设备 

## 服务端要求

为了使用 QRCode Sharer，你需要一个可以处理以下请求的服务：

### API 接口

#### 测试连接

```
GET /
```

| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 用户 ID |
| auth | string | 用户认证密钥 |

#### 获取二维码内容

```
GET /code/get
```

| 参数 | 类型 | 说明 |
|------|------|------|
| follow_user_id | int | 订阅的用户 ID |
| id | int | 当前用户 ID |
| auth | string | 用户认证密钥 |

响应：

```json
{
  "content": "https://example.com",
  "update_at": 1703577600
}
```

#### 更新二维码内容

```
PATCH /code/patch
```

| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 用户 ID |
| auth | string | 用户认证密钥 |

请求体：

```json
{
  "content": "https://example.com"
}
```

#### 获取用户信息

```
GET /user/get
```

| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 当前用户 ID |
| auth | string | 用户认证密钥 |
| check_id | int | 要检查的用户 ID |

## 配置说明

在应用设置中配置以下内容：

- **User ID**: 你的用户 ID
- **User Auth**: 你的认证密钥
- **主机地址**: 服务端地址 (如 `https://api.example.com`)
- **连接超时**: 请求超时时间 (毫秒)
- **请求间隔**: 同步轮询间隔 (毫秒)

## 构建

本项目使用 Kotlin + Jetpack Compose 开发。

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```