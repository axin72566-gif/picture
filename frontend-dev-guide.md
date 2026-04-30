# 图片管理平台 — 前端开发文档

> 根据后端源码自动生成，版本对应 `picture-backend v0.0.1-SNAPSHOT`
> 后端服务地址：`http://localhost:8123`，所有接口统一前缀 `/api`

---

## 目录

1. [请求约定](#一请求约定)
2. [通用数据结构](#二通用数据结构)
3. [错误码说明](#三错误码说明)
4. [枚举常量](#四枚举常量)
5. [TypeScript 类型定义](#五typescript-类型定义)
6. [用户模块](#六用户模块)
7. [图片模块](#七图片模块)
8. [空间模块](#八空间模块)
9. [空间成员模块](#九空间成员模块)
10. [评论模块](#十评论模块)
11. [系统通知模块](#十一系统通知模块)
12. [WebSocket 协同编辑](#十二websocket-协同编辑)
13. [权限控制说明](#十三权限控制说明)
14. [前端推荐目录结构](#十四前端推荐目录结构)

---

## 一、请求约定

| 项目 | 说明 |
|---|---|
| 基础地址 | `http://localhost:8123/api` |
| 认证方式 | Cookie Session（登录后自动携带，无需手动处理 Token） |
| Content-Type | `application/json`（文件上传使用 `multipart/form-data`） |
| Long 类型精度 | 后端 Long 类型字段序列化为 **字符串**（防止 JS 精度丢失），前端接收时注意 `id` 字段类型为 `string` |
| 分页排序默认值 | `current=1`，`pageSize=10`，`sortOrder="descend"` |

### Axios 封装示例

```typescript
import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8123/api',
  withCredentials: true, // 携带 Cookie
  timeout: 10000,
})

// 响应拦截：统一处理业务错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
      // 未登录跳转
      if (res.code === 40100) {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  (error) => Promise.reject(error)
)

export default request
```

---

## 二、通用数据结构

### BaseResponse\<T\>（统一响应体）

```typescript
interface BaseResponse<T> {
  code: number    // 0 表示成功，非 0 表示失败
  data: T         // 业务数据
  message: string // 提示信息
}
```

### PageRequest（分页请求基类）

所有分页查询请求均继承此结构：

```typescript
interface PageRequest {
  current?: number    // 当前页，默认 1
  pageSize?: number   // 每页数量，默认 10
  sortField?: string  // 排序字段名
  sortOrder?: 'ascend' | 'descend'  // 排序方向，默认 descend
}
```

### Page\<T\>（分页响应）

```typescript
interface Page<T> {
  records: T[]   // 当前页数据列表
  total: number  // 总记录数
  size: number   // 每页大小
  current: number // 当前页
  pages: number  // 总页数
}
```

### DeleteRequest（通用删除请求）

```typescript
interface DeleteRequest {
  id: string | number
}
```

---

## 三、错误码说明

| code | 含义 | 前端处理建议 |
|---|---|---|
| `0` | 成功 | 正常处理 |
| `40000` | 请求参数错误 | 提示用户检查输入 |
| `40100` | 未登录 | 跳转登录页 |
| `40101` | 无权限 | 提示无权限或隐藏对应按钮 |
| `40300` | 禁止访问 | 403 提示 |
| `40400` | 数据不存在 | 提示"内容不存在" |
| `50000` | 系统内部异常 | 提示"系统繁忙，请稍后重试" |
| `50001` | 操作失败 | 提示具体 message 内容 |

---

## 四、枚举常量

### 用户角色

```typescript
enum UserRole {
  USER = 'user',   // 普通用户
  ADMIN = 'admin', // 管理员
}
```

### 图片审核状态

```typescript
enum PictureReviewStatus {
  REVIEWING = 0, // 待审核
  PASS = 1,      // 通过
  REJECT = 2,    // 拒绝
}
```

### 空间级别

```typescript
enum SpaceLevel {
  COMMON = 0,       // 普通版：最多 100 张，100MB
  PROFESSIONAL = 1, // 专业版：最多 1000 张，1000MB
  FLAGSHIP = 2,     // 旗舰版：最多 10000 张，10000MB
}
```

### 空间类型

```typescript
enum SpaceType {
  PRIVATE = 0, // 私有空间
  TEAM = 1,    // 团队空间
}
```

### 空间角色

```typescript
enum SpaceRole {
  VIEWER = 'viewer',  // 浏览者：只可查看
  EDITOR = 'editor',  // 编辑者：可上传/编辑图片
  ADMIN = 'admin',    // 管理员：全部权限
}
```

### 空间权限列表（permissionList 字段中的值）

```typescript
const SpacePermission = {
  PICTURE_VIEW: 'picture:view',
  PICTURE_UPLOAD: 'picture:upload',
  PICTURE_EDIT: 'picture:edit',
  PICTURE_DELETE: 'picture:delete',
  SPACE_USER_MANAGE: 'spaceUser:manage',
}
```

---

## 五、TypeScript 类型定义

### UserVO（用户视图，列表/他人信息）

```typescript
interface UserVO {
  id: string
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: 'user' | 'admin'
  createTime: string
}
```

### LoginUserVO（当前登录用户）

```typescript
interface LoginUserVO {
  id: string
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: 'user' | 'admin'
}
```

### PictureVO（图片视图）

```typescript
interface PictureVO {
  id: string
  url: string           // 原图地址
  thumbnailUrl: string  // 缩略图地址
  name: string          // 图片名称
  introduction: string  // 简介
  tags: string[]        // 标签列表（后端 JSON 数组转换）
  category: string      // 分类
  picSize: number       // 文件体积（字节）
  picWidth: number      // 图片宽度（px）
  picHeight: number     // 图片高度（px）
  picScale: number      // 宽高比
  picFormat: string     // 图片格式（如 jpg/png）
  userId: string        // 上传者 ID
  spaceId: string | null // 所属空间 ID，null 表示公共图库
  createTime: string
  editTime: string
  updateTime: string
  user: UserVO          // 上传者信息
  permissionList: string[] // 当前用户对此图片的权限列表
  likeCount: number     // 点赞数
  isLiked: boolean      // 当前用户是否已点赞
}
```

### SpaceVO（空间视图）

```typescript
interface SpaceVO {
  id: string
  spaceName: string
  spaceLevel: 0 | 1 | 2     // 0普通 1专业 2旗舰
  spaceType: 0 | 1           // 0私有 1团队
  maxSize: number            // 最大存储（字节）
  maxCount: number           // 最大图片数量
  totalSize: number          // 当前已用存储（字节）
  totalCount: number         // 当前图片数量
  userId: string             // 创建者 ID
  createTime: string
  editTime: string
  updateTime: string
  user: UserVO               // 创建者信息
  permissionList: string[]   // 当前用户在此空间的权限列表
}
```

### SpaceUserVO（空间成员视图）

```typescript
interface SpaceUserVO {
  id: string
  spaceId: string
  userId: string
  spaceRole: 'viewer' | 'editor' | 'admin'
  createTime: string
  updateTime: string
  user: UserVO
  space: SpaceVO
}
```

### CommentVO（评论视图，树形）

```typescript
interface CommentVO {
  id: string
  pictureId: string
  parentId: string   // "0" 表示一级评论
  content: string
  createTime: string
  user: UserVO
  children: CommentVO[] // 子评论（内存组装的树形结构）
}
```

### NoticeVO（通知视图）

```typescript
interface NoticeVO {
  id: string
  title: string
  content: string
  relatedId: string  // 关联业务 ID（如图片 ID）
  isRead: 0 | 1      // 0未读 1已读
  readTime: string | null
  createTime: string
}
```

---

## 六、用户模块

**Base Path：** `/api/user`

---

### 注册

- **POST** `/api/user/register`
- 权限：公开

**请求体：**
```typescript
interface UserRegisterRequest {
  userAccount: string   // 账号
  userPassword: string  // 密码
  checkPassword: string // 确认密码
}
```

**响应：** `BaseResponse<number>` （返回新用户 ID）

---

### 登录

- **POST** `/api/user/login`
- 权限：公开

**请求体：**
```typescript
interface UserLoginRequest {
  userAccount: string
  userPassword: string
}
```

**响应：** `BaseResponse<LoginUserVO>`

---

### 注销

- **POST** `/api/user/logout`
- 权限：已登录

**请求体：** 无

**响应：** `BaseResponse<boolean>`

---

### 获取当前登录用户

- **GET** `/api/user/get/login`
- 权限：已登录

**响应：** `BaseResponse<LoginUserVO>`

---

### 删除用户（管理员）

- **POST** `/api/user/delete`
- 权限：admin

**请求体：**
```typescript
{ id: string }
```

**响应：** `BaseResponse<boolean>`

---

### 分页查询用户列表（管理员）

- **GET** `/api/user/list/page/vo`
- 权限：admin

**请求参数（Query String）：**
```typescript
interface UserQueryRequest extends PageRequest {
  id?: string
  userAccount?: string
  userName?: string
  userProfile?: string
  userRole?: 'user' | 'admin'
}
```

**响应：** `BaseResponse<Page<UserVO>>`

---

## 七、图片模块

**Base Path：** `/api/picture`

---

### 文件上传图片

- **POST** `/api/picture/upload`
- 权限：空间上传权限（`picture:upload`）
- Content-Type：`multipart/form-data`

**表单字段：**
```
file: File              // 图片文件（必填）
id?: string             // 图片 ID（更新时传入）
picName?: string        // 图片名称
spaceId?: string        // 目标空间 ID（不传则上传到公共图库）
```

**响应：** `BaseResponse<PictureVO>`

---

### URL 上传图片

- **POST** `/api/picture/upload/url`
- 权限：空间上传权限（`picture:upload`）
- Content-Type：`application/json`

**请求体：**
```typescript
interface PictureUploadRequest {
  id?: string      // 图片 ID（更新时传入）
  fileUrl: string  // 图片 URL
  picName?: string // 图片名称
  spaceId?: string // 目标空间 ID
}
```

**响应：** `BaseResponse<PictureVO>`

---

### 批量抓取上传（管理员）

- **POST** `/api/picture/upload/batch`
- 权限：admin

**请求体：**
```typescript
interface PictureUploadByBatchRequest {
  searchText: string   // 搜索关键词（从必应搜索）
  count?: number       // 抓取数量，默认 10
  namePrefix?: string  // 图片名称前缀
}
```

**响应：** `BaseResponse<number>` （成功上传数量）

---

### 删除图片

- **POST** `/api/picture/delete`
- 权限：空间删除权限（`picture:delete`）或本人上传

**请求体：**
```typescript
{ id: string }
```

**响应：** `BaseResponse<boolean>`

---

### 编辑图片（普通用户）

- **POST** `/api/picture/edit`
- 权限：空间编辑权限（`picture:edit`）

**请求体：**
```typescript
interface PictureEditRequest {
  id: string
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
}
```

**响应：** `BaseResponse<boolean>`

---

### 更新图片（管理员）

- **POST** `/api/picture/update`
- 权限：admin

**请求体：**
```typescript
interface PictureUpdateRequest {
  id: string
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
}
```

**响应：** `BaseResponse<boolean>`

---

### 获取图片 VO（单张）

- **GET** `/api/picture/get/vo?id={id}`
- 权限：已登录

**响应：** `BaseResponse<PictureVO>`

---

### 分页获取图片 VO 列表

- **POST** `/api/picture/list/page/vo`
- 权限：已登录

**请求体：**
```typescript
interface PictureQueryRequest extends PageRequest {
  id?: string
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
  picSize?: number
  picWidth?: number
  picHeight?: number
  picScale?: number
  picFormat?: string
  searchText?: string   // 模糊搜索（名称+简介）
  userId?: string
  reviewStatus?: 0 | 1 | 2
  reviewMessage?: string
  reviewerId?: string
  spaceId?: string      // 指定空间；不传则查公共图库
  nullSpaceId?: boolean // true 时只查 spaceId 为空的图片（公共图库）
}
```

**响应：** `BaseResponse<Page<PictureVO>>`

---

### 获取图片标签和分类（公开）

- **GET** `/api/picture/tag_category`
- 权限：公开

**响应：**
```typescript
BaseResponse<{
  tagList: string[]      // 标签列表
  categoryList: string[] // 分类列表
}>
```

---

### 图片审核（管理员）

- **POST** `/api/picture/review`
- 权限：admin

**请求体：**
```typescript
interface PictureReviewRequest {
  id: string
  reviewStatus: 0 | 1 | 2  // 0待审核 1通过 2拒绝
  reviewMessage?: string    // 审核备注
}
```

**响应：** `BaseResponse<boolean>`

---

### 点赞 / 取消点赞

- **POST** `/api/picture/like`
- 权限：已登录

**请求体：**
```typescript
interface PictureLikeRequest {
  pictureId: string
}
```

**响应：** `BaseResponse<boolean>`（`true` 已点赞，`false` 已取消）

---

## 八、空间模块

**Base Path：** `/api/space`

---

### 创建空间

- **POST** `/api/space/add`
- 权限：已登录

**请求体：**
```typescript
interface SpaceAddRequest {
  spaceName: string
  spaceLevel?: 0 | 1 | 2  // 默认 0（普通版）
  spaceType?: 0 | 1        // 默认 0（私有）
}
```

> 团队空间（`spaceType=1`）创建后会自动将创建者设为 admin 成员。

**响应：** `BaseResponse<string>` （新空间 ID）

---

### 编辑空间

- **POST** `/api/space/edit`
- 权限：空间 admin

**请求体：**
```typescript
interface SpaceEditRequest {
  id: string
  spaceName?: string
}
```

**响应：** `BaseResponse<boolean>`

---

### 更新空间（管理员）

- **POST** `/api/space/update`
- 权限：admin

**请求体：**
```typescript
interface SpaceUpdateRequest {
  id: string
  spaceName?: string
  spaceLevel?: 0 | 1 | 2
  maxSize?: number
  maxCount?: number
}
```

**响应：** `BaseResponse<boolean>`

---

### 删除空间

- **POST** `/api/space/delete`
- 权限：空间 admin 或 系统 admin

**请求体：**
```typescript
{ id: string }
```

**响应：** `BaseResponse<boolean>`

---

### 获取空间 VO（单个）

- **GET** `/api/space/get/vo?id={id}`
- 权限：已登录

**响应：** `BaseResponse<SpaceVO>`

---

### 分页查询空间列表

- **POST** `/api/space/list/page/vo`
- 权限：已登录

**请求体：**
```typescript
interface SpaceQueryRequest extends PageRequest {
  id?: string
  userId?: string
  spaceName?: string
  spaceLevel?: 0 | 1 | 2
  spaceType?: 0 | 1
}
```

**响应：** `BaseResponse<Page<SpaceVO>>`

---

### 获取空间级别配置

- **GET** `/api/space/list/level`
- 权限：已登录

**响应：**
```typescript
BaseResponse<Array<{
  value: number    // 0/1/2
  text: string     // 普通版/专业版/旗舰版
  maxCount: number // 最大图片数量
  maxSize: number  // 最大存储字节数
}>>
```

---

## 九、空间成员模块

**Base Path：** `/api/spaceUser`

---

### 添加成员

- **POST** `/api/spaceUser/add`
- 权限：空间 admin

**请求体：**
```typescript
interface SpaceUserAddRequest {
  spaceId: string
  userId: string
  spaceRole?: 'viewer' | 'editor' | 'admin'  // 默认 viewer
}
```

**响应：** `BaseResponse<string>` （新记录 ID）

---

### 编辑成员角色

- **POST** `/api/spaceUser/edit`
- 权限：空间 admin

**请求体：**
```typescript
interface SpaceUserEditRequest {
  id: string
  spaceRole: 'viewer' | 'editor' | 'admin'
}
```

**响应：** `BaseResponse<boolean>`

---

### 删除成员

- **POST** `/api/spaceUser/delete`
- 权限：空间 admin

**请求体：**
```typescript
{ id: string }
```

**响应：** `BaseResponse<boolean>`

---

### 查询成员列表

- **POST** `/api/spaceUser/list`
- 权限：空间成员

**请求体：**
```typescript
interface SpaceUserQueryRequest extends PageRequest {
  id?: string
  spaceId?: string
  userId?: string
  spaceRole?: 'viewer' | 'editor' | 'admin'
}
```

**响应：** `BaseResponse<SpaceUserVO[]>`

---

### 获取当前用户在空间中的权限

- **GET** `/api/spaceUser/get/my?spaceId={spaceId}`
- 权限：已登录

**响应：** `BaseResponse<SpaceUserVO>`

---

## 十、评论模块

**Base Path：** `/api/comment`

---

### 发布评论

- **POST** `/api/comment/add`
- 权限：已登录

**请求体：**
```typescript
interface CommentAddRequest {
  pictureId: string
  parentId?: string  // 不传或传 "0" 表示一级评论；传父评论 ID 表示回复
  content: string
}
```

**响应：** `BaseResponse<string>` （新评论 ID）

---

### 查询评论列表（树形）

- **POST** `/api/comment/list`
- 权限：已登录

**请求体：**
```typescript
interface CommentQueryRequest extends PageRequest {
  pictureId: string
}
```

**响应：** `BaseResponse<CommentVO[]>`

> 返回的是树形结构：一级评论的 `children` 字段包含其所有回复。

---

### 删除评论

- **POST** `/api/comment/delete`
- 权限：评论本人或 admin

**请求体：**
```typescript
{ id: string }
```

**响应：** `BaseResponse<boolean>`

---

## 十一、系统通知模块

**Base Path：** `/api/notice`

---

### 查询通知列表

- **POST** `/api/notice/list`
- 权限：已登录（只返回当前用户的通知）

**请求体：**
```typescript
interface NoticeQueryRequest extends PageRequest {
  isRead?: 0 | 1  // 0未读 1已读 不传则查全部
}
```

**响应：** `BaseResponse<Page<NoticeVO>>`

---

### 标记通知已读

- **POST** `/api/notice/read`
- 权限：已登录

**请求体：**
```typescript
interface NoticeReadRequest {
  id?: string    // 不传则标记全部已读
}
```

**响应：** `BaseResponse<boolean>`

---

### 获取未读通知数量

- **GET** `/api/notice/unread/count`
- 权限：已登录

**响应：** `BaseResponse<number>`

---

## 十二、WebSocket 协同编辑

### 连接地址

```
ws://localhost:8123/api/ws/picture/edit/{pictureId}
```

> 握手时服务端会校验用户登录状态和图片编辑权限（`picture:edit`），未登录或无权限会拒绝握手。

---

### 客户端 → 服务端（发送消息）

```typescript
interface PictureEditRequestMessage {
  type: 'ENTER_EDIT' | 'EXIT_EDIT' | 'EDIT_ACTION'
  editAction?: 'ZOOM_IN' | 'ZOOM_OUT' | 'ROTATE_LEFT' | 'ROTATE_RIGHT'
}
```

| `type` | `editAction` | 说明 |
|---|---|---|
| `ENTER_EDIT` | - | 进入编辑状态（通知其他在线用户） |
| `EXIT_EDIT` | - | 退出编辑状态 |
| `EDIT_ACTION` | 必填 | 执行具体编辑操作并广播给其他用户 |

**editAction 说明：**

| 值 | 说明 |
|---|---|
| `ZOOM_IN` | 放大 |
| `ZOOM_OUT` | 缩小 |
| `ROTATE_LEFT` | 左旋 90° |
| `ROTATE_RIGHT` | 右旋 90° |

---

### 服务端 → 客户端（接收消息）

```typescript
interface PictureEditResponseMessage {
  type: 'INFO' | 'ERROR' | 'ENTER_EDIT' | 'EXIT_EDIT' | 'EDIT_ACTION'
  message?: string   // 提示文字（INFO/ERROR 类型时有值）
  editAction?: string // 编辑动作（EDIT_ACTION 时有值）
  user?: UserVO      // 触发该事件的用户信息
}
```

| `type` | 说明 | 前端处理建议 |
|---|---|---|
| `INFO` | 普通通知（如"xxx 进入/退出编辑"） | Toast 提示 |
| `ERROR` | 错误提示 | Toast 错误提示 |
| `ENTER_EDIT` | 某用户进入编辑 | 显示编辑者头像/名称 |
| `EXIT_EDIT` | 某用户退出编辑 | 移除编辑者标记 |
| `EDIT_ACTION` | 某用户执行了编辑操作 | 同步执行该操作到本地画布 |

### 前端 WebSocket 使用示例

```typescript
const ws = new WebSocket(`ws://localhost:8123/api/ws/picture/edit/${pictureId}`)

ws.onopen = () => {
  // 进入编辑
  ws.send(JSON.stringify({ type: 'ENTER_EDIT' }))
}

ws.onmessage = (event) => {
  const msg: PictureEditResponseMessage = JSON.parse(event.data)
  switch (msg.type) {
    case 'INFO':
      showToast(msg.message)
      break
    case 'EDIT_ACTION':
      applyEditAction(msg.editAction) // 同步操作到本地
      break
    case 'ENTER_EDIT':
      showEditorBadge(msg.user)
      break
    case 'EXIT_EDIT':
      hideEditorBadge(msg.user)
      break
  }
}

// 发送编辑操作
function sendEditAction(action: string) {
  ws.send(JSON.stringify({ type: 'EDIT_ACTION', editAction: action }))
}

// 离开时退出编辑
ws.send(JSON.stringify({ type: 'EXIT_EDIT' }))
ws.close()
```

---

## 十三、权限控制说明

### 全局角色权限

| 角色 | 值 | 说明 |
|---|---|---|
| 普通用户 | `user` | 可使用大多数功能 |
| 管理员 | `admin` | 可访问所有管理接口 |

前端判断管理员：
```typescript
const isAdmin = loginUser.value?.userRole === 'admin'
```

### 空间权限（permissionList）

`PictureVO` 和 `SpaceVO` 均包含 `permissionList` 字段，表示**当前登录用户**对该资源的权限列表。

```typescript
// 判断是否有编辑权限
const canEdit = pictureVO.permissionList.includes('picture:edit')

// 判断是否有删除权限
const canDelete = pictureVO.permissionList.includes('picture:delete')

// 判断是否有上传权限
const canUpload = spaceVO.permissionList.includes('picture:upload')
```

### 空间角色与权限对照

| 空间角色 | 可执行操作 |
|---|---|
| `viewer`（浏览者） | `picture:view` |
| `editor`（编辑者） | `picture:view`、`picture:upload`、`picture:edit` |
| `admin`（管理员） | 全部权限，包括 `picture:delete`、`spaceUser:manage` |

---

## 十四、前端推荐目录结构

```
picture-frontend/
├── src/
│   ├── api/                      # 接口层
│   │   ├── user.ts               # 用户接口
│   │   ├── picture.ts            # 图片接口
│   │   ├── space.ts              # 空间接口
│   │   ├── spaceUser.ts          # 空间成员接口
│   │   ├── comment.ts            # 评论接口
│   │   └── notice.ts             # 通知接口
│   │
│   ├── types/                    # TypeScript 类型定义
│   │   ├── common.ts             # BaseResponse、Page、PageRequest
│   │   ├── user.ts               # UserVO、LoginUserVO、请求 DTO
│   │   ├── picture.ts            # PictureVO、请求 DTO
│   │   ├── space.ts              # SpaceVO、SpaceUserVO、请求 DTO
│   │   ├── comment.ts            # CommentVO、请求 DTO
│   │   ├── notice.ts             # NoticeVO、请求 DTO
│   │   └── websocket.ts          # WS 消息类型
│   │
│   ├── constants/                # 枚举常量
│   │   ├── userRole.ts
│   │   ├── pictureReviewStatus.ts
│   │   ├── spaceLevel.ts
│   │   ├── spaceType.ts
│   │   └── spaceRole.ts
│   │
│   ├── stores/                   # Pinia 状态管理
│   │   ├── user.ts               # 当前登录用户状态
│   │   └── notice.ts             # 未读通知数状态
│   │
│   ├── utils/
│   │   ├── request.ts            # Axios 封装
│   │   └── permission.ts         # 权限判断工具函数
│   │
│   ├── views/
│   │   ├── LoginView.vue         # 登录页
│   │   ├── RegisterView.vue      # 注册页
│   │   ├── HomeView.vue          # 公共图片广场
│   │   ├── PictureDetailView.vue # 图片详情（含评论、点赞、协同编辑）
│   │   ├── space/
│   │   │   ├── SpaceListView.vue # 我的空间列表
│   │   │   ├── SpaceDetailView.vue # 空间图片管理
│   │   │   └── SpaceMemberView.vue # 团队成员管理
│   │   └── admin/
│   │       ├── UserManageView.vue    # 用户管理
│   │       ├── PictureReviewView.vue # 图片审核
│   │       └── BatchUploadView.vue   # 批量抓取
│   │
│   └── router/
│       └── index.ts              # 路由配置（含权限守卫）
```

### 路由权限守卫示例

```typescript
router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 未登录且访问需要登录的页面
  if (to.meta.requiresAuth && !userStore.loginUser) {
    return '/login'
  }

  // 需要 admin 权限
  if (to.meta.requiresAdmin && userStore.loginUser?.userRole !== 'admin') {
    return '/403'
  }
})
```

---

> 文档生成时间：2026-04-27  
> 对应后端版本：`picture-backend v0.0.1-SNAPSHOT`  
> 如后端接口有变更，请同步更新此文档。
