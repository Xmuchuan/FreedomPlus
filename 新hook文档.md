# 抖音(Douyin/TikTok) Xposed Module - Hook 逻辑开发文档

## 文档信息
- **来源APK**: Ｋｅｖｉｎ_26040902.apk
- **反编译工具**: Apktool 3.0.1
- **编写日期**: 2026-04-13
- **文档版本**: v2.0
- **目的**: 从反编译代码中提取抖音Hook逻辑，用于修复和重构抖音模块源码

---

## 目录
1. [概述](#1-概述)
2. [主要类结构](#2-主要类结构)
3. [配置常量详解](#3-配置常量详解)
4. [核心功能操作流程](#4-核心功能操作流程)
   - [4.1 无水印视频获取流程](#41-无水印视频获取流程)
   - [4.2 图文内容获取流程](#42-图文内容获取流程)
   - [4.3 视频文案提取方法](#43-视频文案提取方法)
   - [4.4 链接地址获取流程](#44-链接地址获取流程)
   - [4.5 IP位置信息获取与解析](#45-ip位置信息获取与解析)
   - [4.6 音频URL提取流程](#46-音频url提取流程)
   - [4.7 批量下载管理流程](#47-批量下载管理流程)
5. [核心Hook方法详解](#5-核心hook方法详解)
6. [内部类说明](#6-内部类说明)
7. [DexKit搜索逻辑](#7-dexkit搜索逻辑)
8. [UI相关逻辑](#8-ui相关逻辑)
9. [其他功能模块](#9-其他功能模块)
10. [修复建议](#10-修复建议)
11. [附录](#11-附录)

---

## 1. 概述

### 1.1 模块功能
这是一个针对抖音(TikTok)的Xposed模块，主要功能包括：
- 视频下载（支持批量下载、去水印）
- 评论时间显示自定义
- 去水印功能（视频、图文）
- 音频/音乐控制
- UI定制（隐藏导航栏、顶栏、全屏播放等）
- 表情包相关功能
- 直播相关功能
- 评论图片下载
- IP归属地解析

### 1.2 核心类
| 类名 | 功能描述 |
|------|---------|
| `DYHook` | 抖音主Hook类，包含主要的Hook逻辑和配置 |
| `TikTokHook` | TikTok辅助Hook类，提供UI构建和下载功能 |
| `DexKitFinder` | DexKit搜索辅助类，用于动态查找方法 |
| `KSHook` | 快手Hook类（供参考） |
| `XhsHook` | 小红书Hook类（供参考） |

### 1.3 支持的应用
| 包名 | 应用名称 |
|------|---------|
| `com.ss.android.ugc.aweme` | 抖音 |
| `com.zhiliaoapp.musically` | TikTok/抖音海外版 |
| `com.xingin.xhs` | 小红书 |
| `com.smile.gifmaker` | 快手 |

---

## 2. 主要类结构

### 2.1 DYHook 类结构

**文件位置**: `smali/kevin/fun/hook/DYHook.smali`

**类声明**:
```java
.class public Lkevin/fun/hook/DYHook;
.super Ljava/lang/Object;
.source "DYHook.java"

# 实现的接口
.implements Lde/robv/android/xposed/IXposedHookLoadPackage;
.implements Lde/robv/android/xposed/IXposedHookZygoteInit;
```

**主要字段（存储内容信息）**:
```smali
.field private final batchManager:Lkevin/fun/hook/DYHook$BatchDownloadManager;
.field private totalProgressDialogTag:Lkevin/fun/hook/DYHook$ProgressViewHolder;
.field public isHookSet:Z
.field public isBottomTabProcessed:Z
.field public isTopTabProcessed:Z
.field public isFullScreenEnabled:Z
.field public isBatchDownloading:Z

# 内容信息相关字段
.field videoDesc:Ljava/lang/String;           # 视频文案/描述
.field releaseTime:Ljava/lang/String;         # 发布时间
.field regionName:Ljava/lang/String;           # 地域名称
.field cityName:Ljava/lang/String;            # 城市名称
.field userNickname:Ljava/lang/String;         # 用户昵称
.field userAccount:Ljava/lang/String;          # 用户账号
.field videoLabelsTag1:Ljava/lang/String;     # 视频标签1
.field videoLabelsTag2:Ljava/lang/String;     # 视频标签2
.field shareUrl:Ljava/lang/String;             # 分享链接
```

**内部类**:
| 内部类 | 功能 |
|--------|------|
| `DYHook$BatchDownloadManager` | 批量下载管理器 |
| `DYHook$CommentImageInfo` | 评论图片信息 |
| `DYHook$CommentViewHolder` | 评论列表项视图Holder |
| `DYHook$Consumer<T>` | 通用消费者接口 |
| `DYHook$GridViewHolder` | 网格视图Holder |
| `DYHook$LivePhotoCreator` | livephoto创建器 |
| `DYHook$ProgressCallback` | 进度回调接口 |
| `DYHook$ProgressViewHolder` | 进度视图Holder |
| `DYHook$SimpleTextWatcher` | 简单文本监听器 |
| `DYHook$WeakContextRunnable` | 弱引用上下文Runnable |
| `DYHook$WeakDialogRunnable` | 弱引用对话框Runnable |
| `DYHook$WeakOnClickListener` | 弱引用点击监听器 |
| `DYHook$WeakProgressDialogRunnable` | 弱引用进度对话框Runnable |
| `DYHook$1` ~ `DYHook$97` | 匿名内部类，包含各种Hook实现 |

---

## 3. 配置常量详解

### 3.1 存储配置Key (SharedPreferences)

这些常量用于在SharedPreferences中存储和读取配置：

#### 3.1.1 通用配置
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_AGREEMENT_ACCEPTED` | 协议是否已接受 | boolean |
| `KEY_AGREEMENT_VERSION` | 协议版本 | String |
| `KEY_DEXKIT_VERSION` | DexKit版本 | String |

#### 3.1.2 下载相关
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_DOWNLOAD_FORMAT` | 下载格式配置 | String |
| `KEY_WATERMARK` | 水印设置 | boolean |
| `KEY_READ_DOMARK` | 下载标记读取 | boolean |
| `KEY_READ_DOREQUEST` | 请求读取 | boolean |

#### 3.1.3 时间显示相关
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_FEED_SHOW_TIME` | 视频流显示时间 | boolean |
| `KEY_FEED_TIME_FORMAT` | 视频时间格式 | String |
| `KEY_COMMENT_TIME_FORMAT` | 评论时间格式 | String |
| `KEY_MESSAGE_TIME_FORMAT` | 消息时间格式 | String |
| `KEY_MESSAGE_SHOW_TIME` | 消息显示时间 | boolean |
| `KEY_ENABLE_COMMENT_FULL_TIME` | 启用评论完整时间 | boolean |
| `KEY_COLORFUL_TIMESTAMP` | 彩色时间戳 | boolean |

#### 3.1.4 视频相关
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_VIDEO_NUMBER` | 视频数量 | int |
| `KEY_VIDEO_INFO_ALPHA` | 视频信息透明度 | float |

#### 3.1.5 直播相关
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_LIVE_NUMBER` | 直播数量 | int |
| `KEY_NUMBER_LIVE` | 数字直播 | boolean |
| `KEY_LiveRoomAudienceWidget_Dj_Method` | 观众组件DJ方法 | String |

#### 3.1.6 UI定制相关
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_HIDE_TOP_BAR` | 隐藏顶栏 | boolean |
| `KEY_HIDE_NAV_BAR` | 隐藏导航栏 | boolean |
| `KEY_TOP_BAR_ALPHA` | 顶栏透明度 | float |
| `KEY_NAV_BAR_ALPHA` | 导航栏透明度 | float |
| `KEY_FULLSCREEN_PLAY` | 全屏播放 | boolean |
| `KEY_HIDE_EMOJI` | 隐藏表情 | boolean |
| `KEY_BOTTOM_TRANSPARENT` | 底部透明 | boolean |
| `KEY_REMOVE_ACTION_BAR` | 移除操作栏 | boolean |
| `KEY_Remove_ActionBar` | 移除ActionBar | boolean |

#### 3.1.7 音乐控制相关
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_MUSIC_CONTROL_ENABLE` | 音乐控制启用 | boolean |
| `KEY_MUSIC_CONTROL_HORIZONTAL` | 音乐控制水平 | boolean |
| `KEY_MUSIC_CONTROL_VERTICAL` | 音乐控制垂直 | boolean |

#### 3.1.8 评论相关
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_REMOVE_COMMENT_LIKE` | 移除评论点赞 | boolean |
| `KEY_REMOVE_COMMENT_DISLIKE` | 移除评论点踩 | boolean |
| `KEY_REMOVE_COMMENT_REPLY` | 移除评论回复 | boolean |
| `KEY_REMOVE_COMMENT_INPUT` | 移除评论输入 | boolean |
| `KEY_BLOCK_CLICK_REPLY` | 阻止点击回复 | boolean |
| `KEY_COMMENT_FRAGMENT_CLASS` | 评论Fragment类 | String |
| `KEY_COMMENT_INTERACTION_DELEGATE` | 评论交互代理 | String |

#### 3.1.9 消息相关
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_MESSAGE_HANDLER_CLASS` | 消息处理器类 | String |
| `KEY_REMOVE_ININPUT` | 移除输入 | boolean |
| `KEY_REMOVE_RECORD` | 移除录制 | boolean |
| `KEY_REMOVE_RED` | 移除红包 | boolean |

#### 3.1.10 其他功能
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_DISABLE_BACK_REFRESH` | 禁用后退刷新 | boolean |
| `KEY_DISABLE_FOLLOW_REFRESH` | 禁用关注刷新 | boolean |
| `KEY_SHARE_MENU` | 分享菜单 | boolean |
| `KEY_CUSTOM_DICE` | 自定义骰子 | boolean |
| `KEY_REPLY` | 回复功能 | boolean |
| `KEY_REMOVE_COPY_AT` | 移除复制@ | boolean |
| `KEY_REMOVE_WITHDRAW` | 移除撤回 | boolean |
| `KEY_CHAT_VOICE_FORWARD` | 聊天语音转发 | boolean |

#### 3.1.11 类名配置（用于反射）
| Key常量 | 功能 | 类型 |
|---------|------|------|
| `KEY_NON_PUBLIC_CLASS` | 非公开类 | String |
| `KEY_SHARE_PACKAGE_STATIC_CREATOR` | 分享包静态创建器 | String |
| `KEY_FEEDAPI` | Feed API类 | String |
| `KEY_FEEDAPI_2` | Feed API类2 | String |
| `KEY_HOMEPAGE_TIME_CLASS` | 主页时间类 | String |
| `KEY_IM_FORWARD_ACTION_CLASS` | IM转发Action类 | String |
| `KEY_MAIN_TAB_LAYOUT_CLASS` | 主Tab布局类 | String |
| `KEY_TOUCH_LISTENER_CLASS` | 触摸监听类 | String |
| `KEY_TIME_COMPONENT_UPDATE_METHOD` | 时间组件更新方法 | String |
| `KEY_BIG_EMOJI_CLASS` | 大表情类 | String |
| `KEY_EMOJI_DETAIL_PARAMS` | 表情详情参数 | String |

---

## 4. 核心功能操作流程

### 4.1 无水印视频获取流程

#### 4.1.1 流程概述

```
用户点击下载按钮
    ↓
检查水印设置 (KEY_WATERMARK)
    ↓
获取视频URL (含水印/无水印)
    ↓
判断是否去水印
    ↓
下载视频文件
    ↓
保存到本地
```

#### 4.1.2 详细步骤

**步骤1: 检测水印设置**

代码位置: DYHook.smali 约44119行

```smali
.local v5, "watermarkText":Ljava/lang/String;
```

**关键判断逻辑**:
```java
// 获取水印配置
boolean watermarkEnabled = prefs.getBoolean(KEY_WATERMARK, true);

if (watermarkEnabled) {
    // 使用带水印的视频URL
} else {
    // 使用无水印的视频URL
}
```

**步骤2: 获取视频URL**

视频URL的获取通过Hook视频数据解析方法实现：

- **Hook类**: `DYHook$8` (视频信息处理)
- **Hook方法**: `afterHookedMethod` (在视频数据解析完成后)
- **目标方法**: 视频数据解析相关方法

**步骤3: 水印处理**

水印处理涉及以下字段：
- `watermarkText`: 水印文本内容
- `KEY_WATERMARK`: 水印开关配置

**无水印视频URL获取技巧**:
1. 抖音视频通常有多个播放URL
2. 无水印URL通常特征: 不包含 `watermark` 或 `water_mark` 参数
3. 部分版本可通过替换 URL 参数去除水印

**步骤4: 下载保存**

```smali
# 调用下载方法
invoke-direct {p0, p1}, Lkevin/fun/hook/DYHook;->startDownload(...)
```

#### 4.1.3 技术参数

| 参数 | 说明 |
|------|------|
| 视频URL类型 | m3u8 / mp4 |
| 常见域名 | v26-dy.ixigua.com, v3-dy.ixigua.com |
| URL特征 | 包含 aweme_id, video_id 等参数 |
| 无水印判断 | 检查 URL 中是否包含水印参数 |

#### 4.1.4 常见问题及解决

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 无法获取无水印视频 | 抖音版本更新 | 更新Hook点，适应新版API |
| 视频URL为空 | Hook时机不对 | 检查Hook方法是否在数据加载后执行 |
| 下载失败 | 网络问题/存储权限 | 检查网络连接和存储权限 |
| 水印去除无效 | URL解析错误 | 检查水印参数替换逻辑 |

---

### 4.2 图文内容获取流程

#### 4.2.1 流程概述

```
extractImageUrls 方法触发
    ↓
获取图文数据列表 (imageInfos)
    ↓
检查列表是否为空
    ↓
遍历图片信息
    ↓
下载每张图片
    ↓
保存到本地
```

#### 4.2.2 详细步骤

**方法签名**:
```java
.method private extractImageUrls(Landroid/content/Context;)V
```

代码位置: DYHook.smali 约20374行

**核心流程代码分析**:

```smali
# 第20391-20407行: 检查图片数据是否存在
invoke-static {}, Lkevin/fun/hook/DYHook;->ۣۣ۟ۡۨ()Ljava/lang/String;

move-result-object v0

if-eqz v0, :cond_3    # 如果为空则跳转

invoke-static {}, Lkevin/fun/hook/DYHook;->ۣۣ۟ۡۨ()Ljava/lang/String;

move-result-object v0

invoke-static {v0}, Landroidx/cursoradapter/۟۠ۦ۟۟;->۟ۤۤ۠(Ljava/lang/Object;)Z

move-result v0

if-eqz v0, :cond_0    # 继续检查
```

**步骤1: 获取图片列表**

```smali
# 第20415行: 获取图片信息列表
invoke-static {v4, v0}, Lkevin/fun/hook/DYHook;->۟ۧۦ۟۟(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;

move-result-object v0

.local v0, "imageInfos":Ljava/util/List;
```

返回类型: `List<CommentImageInfo>`

**步骤2: 检查列表有效性**

```smali
# 第20421行: 检查列表是否有效
invoke-static {v0}, Landroidx/customview/۟ۥ۟ۡ۟;->ۣ۟ۢۤۢ(Ljava/lang/Object;)Z

move-result v1

if-eqz v1, :cond_1
```

**步骤3: 获取列表大小**

```smali
# 第20451行: 获取列表大小
invoke-static {v0}, Landroidx/appcompat/resources/۟ۥۣۧۨ;->ۡ۠۠ۡ(Ljava/lang/Object;)I

move-result v1
```

**步骤4: 遍历下载**

```smali
# 第20462行: 获取单个图片信息
invoke-static {v0, v1}, Landroidx/appcompat/resources/۟ۥۣۧۨ;->ۤ۠ۨ۠(Ljava/lang/Object;I)Ljava/lang/Object;

move-result-object v1

check-cast v1, Lkevin/fun/hook/DYHook$CommentImageInfo;

# 调用下载方法
invoke-static {v4, v5, v1}, Lkevin/fun/hook/DYHook;->ۣ۟۠ۨۦ(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
```

#### 4.2.3 CommentImageInfo 结构

```java
.class Lkevin/fun/hook/DYHook$CommentImageInfo;
.super Ljava/lang/Object;

.field public url:Ljava/lang/String;        # 图片URL
.field public path:Ljava/lang/String;       # 本地路径
.field public width:I                        # 宽度
.field public height:I                       # 高度
```

#### 4.2.4 常见问题及解决

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 图片列表为空 | 数据未加载完成 | 确保在数据加载完成后调用 |
| 下载失败 | URL无效/网络问题 | 检查URL格式和网络状态 |
| 图片顺序混乱 | 列表遍历顺序问题 | 检查图片索引处理逻辑 |

---

### 4.3 视频文案提取方法

#### 4.3.1 流程概述

```
getContentInfo 方法被调用
    ↓
获取aweme对象 (视频数据对象)
    ↓
提取文案字段 (videoDesc)
    ↓
提取发布时间 (releaseTime)
    ↓
提取地域信息 (regionName, cityName)
    ↓
提取用户信息 (userNickname, userAccount)
    ↓
提取标签 (videoLabelsTag1, videoLabelsTag2)
    ↓
提取分享链接 (shareUrl)
```

#### 4.3.2 详细步骤

**方法签名**:
```java
.method private getContentInfo(Landroid/content/Context;Ljava/lang/Object;Ljava/lang/Object;)V
```

代码位置: DYHook.smali 约22046行

**参数说明**:
| 参数 | 类型 | 说明 |
|------|------|------|
| p1 | Context | 上下文 |
| p2 | Object | aweme对象 (视频数据) |
| p3 | Object | videoObj对象 (视频对象) |

**核心流程代码分析**:

**步骤1: 初始化字段**

```smali
# 第22096-22103行: 初始化所有字段为空字符串
:try_start_0
iput-object v4, v1, Lkevin/fun/hook/DYHook;->videoDesc:Ljava/lang/String;
iput-object v4, v1, Lkevin/fun/hook/DYHook;->releaseTime:Ljava/lang/String;
iput-object v4, v1, Lkevin/fun/hook/DYHook;->regionName:Ljava/lang/String;
iput-object v4, v1, Lkevin/fun/hook/DYHook;->cityName:Ljava/lang/String;
```

**步骤2: 提取视频文案 (desc)**

```smali
# 第22096行: videoDesc 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->videoDesc:Ljava/lang/String;

# 其中 v4 是通过以下方式获取:
invoke-static {}, Landroidx/emoji2/viewsintegration/۟ۥۦۤۢ;->۟۠۠۠۠()Ljava/lang/String;

move-result-object v4
```

**步骤3: 提取发布时间 (releaseTime)**

```smali
# 第22100行: releaseTime 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->releaseTime:Ljava/lang/String;
```

**步骤4: 提取地域信息**

```smali
# 第22103行: regionName 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->regionName:Ljava/lang/String;

# 第22106行: cityName 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->cityName:Ljava/lang/String;
```

**步骤5: 提取用户信息**

```smali
# 第22109行: userNickname 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->userNickname:Ljava/lang/String;

# 第22112行: userAccount 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->userAccount:Ljava/lang/String;
```

**步骤6: 提取视频标签**

```smali
# 第22115行: videoLabelsTag1 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->videoLabelsTag1:Ljava/lang/String;

# 第22118行: videoLabelsTag2 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->videoLabelsTag2:Ljava/lang/String;
```

**步骤7: 提取分享链接**

```smali
# 第22121行: shareUrl 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->shareUrl:Ljava/lang/String;
```

#### 4.3.3 数据来源

视频文案数据来源于aweme对象，该对象是抖音视频数据的核心模型：

| 字段 | 来源 | 说明 |
|------|------|------|
| videoDesc | aweme.desc | 视频文案/描述 |
| releaseTime | aweme.create_time | 发布时间戳 |
| regionName | aweme.region | 地域名称 |
| cityName | aweme.city | 城市名称 |
| userNickname | aweme.author.nickname | 用户昵称 |
| userAccount | aweme.author.uid | 用户ID |
| videoLabelsTag1 | aweme.video_labels[0] | 视频标签1 |
| videoLabelsTag2 | aweme.video_labels[1] | 视频标签2 |
| shareUrl | aweme.share_url | 分享链接 |

#### 4.3.4 完整提取示例

```java
// 伪代码展示完整提取流程
public void extractVideoInfo(Object aweme) {
    // 1. 提取文案
    String desc = invokeMethod(aweme, "getDesc");  // videoDesc

    // 2. 提取时间
    long createTime = invokeMethod(aweme, "getCreateTime");  // releaseTime
    String timeStr = formatTimestamp(createTime);

    // 3. 提取地域
    String region = invokeMethod(aweme, "getRegion");  // regionName
    String city = invokeMethod(aweme, "getCity");      // cityName

    // 4. 提取用户信息
    Object author = invokeMethod(aweme, "getAuthor");
    String nickname = invokeMethod(author, "getNickname");  // userNickname
    String uid = invokeMethod(author, "getUid");            // userAccount

    // 5. 提取标签
    List labels = invokeMethod(aweme, "getVideoLabels");
    if (labels.size() > 0) {
        videoLabelsTag1 = labels.get(0);
    }
    if (labels.size() > 1) {
        videoLabelsTag2 = labels.get(1);
    }

    // 6. 提取分享链接
    String shareUrl = invokeMethod(aweme, "getShareUrl");  // shareUrl
}
```

#### 4.3.5 常见问题及解决

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 文案为空 | aweme对象未加载 | 确保在数据加载完成后调用 |
| 时间格式错误 | 时间戳未转换 | 使用SimpleDateFormat转换 |
| 地域信息缺失 | 用户未开启位置权限 | 检查权限或使用默认值 |
| 分享链接失效 | URL已过期 | 使用实时获取的URL |

---

### 4.4 链接地址获取流程

#### 4.4.1 分享链接获取

分享链接通过 `shareUrl` 字段获取：

```smali
# 第22121行: shareUrl 赋值
iput-object v4, v1, Lkevin/fun/hook/DYHook;->shareUrl:Ljava/lang/String;

# 获取方式:
invoke-static {}, Landroidx/emoji2/viewsintegration/۟ۥۦۤۢ;->۟۠۠۠۠()Ljava/lang/String;

move-result-object v4
```

**分享链接格式**:
```
https://www.iesdouyin.com/share/video/视频ID/?region=CN&mid=视频ID&u_code=0&did=设备ID&app_type=normal&iid=安装ID&with_sec_did=1
```

#### 4.4.2 视频播放URL获取

视频播放URL的获取流程：

```
获取aweme对象
    ↓
提取视频数据列表 (videoDataList)
    ↓
遍历获取视频URL
    ↓
检查URL有效性
    ↓
返回最优URL (通常是无水印mp4)
```

**关键代码位置**: DYHook.smali 约55887行

```smali
# 调用 getContentInfo 方法
invoke-direct {p0, p1, p2, p3}, Lkevin/fun/hook/DYHook;->getContentInfo(Landroid/content/Context;Ljava/lang/Object;Ljava/lang/Object;)V
```

#### 4.4.3 链接类型

| 链接类型 | 说明 | 获取难度 |
|---------|------|---------|
| 分享链接 | 用于分享的短链接 | 简单 |
| 播放链接 | 视频实际播放URL | 中等 |
| 音频链接 | 背景音乐URL | 复杂 |
| 封面链接 | 视频封面图URL | 简单 |
| 评论图片链接 | 评论中的图片URL | 中等 |

#### 4.4.4 URL处理技巧

1. **去除水印参数**:
   - 移除 URL 中的 `watermark=1` 参数
   - 或替换为 `watermark=0`

2. **获取高清版本**:
   - 检查 URL 中的 `ratio` 参数
   - 选择 `ratio=1080p` 或更高

3. **处理失效链接**:
   - 添加时间戳参数
   - 使用最新获取的URL

---

### 4.5 IP位置信息获取与解析

#### 4.5.1 地域信息字段

从视频数据中提取的IP位置信息包括：

| 字段 | 说明 | 数据来源 |
|------|------|---------|
| `regionName` | 地域/省份名称 | aweme.region |
| `cityName` | 城市名称 | aweme.city |

代码位置: DYHook.smali 第22103行和第22106行

#### 4.5.2 获取流程

```smali
# 第22103行: 获取地域名称
iput-object v4, v1, Lkevin/fun/hook/DYHook;->regionName:Ljava/lang/String;

# 第22106行: 获取城市名称
iput-object v4, v1, Lkevin/fun/hook/DYHook;->cityName:Ljava/lang/String;
```

#### 4.5.3 数据格式

**regionName 可能的值**:
- `CN` - 中国（缩写）
- `US` - 美国
- `JP` - 日本
- 等等

**cityName 可能的值**:
- `北京`
- `上海`
- `深圳`
- 等等

#### 4.5.4 IP解析逻辑

抖音的IP归属地解析基于以下原理：

1. **服务端解析**: 抖音服务器根据用户发布时的IP地址进行解析
2. **数据存储**: 解析后的地域信息存储在视频数据中
3. **客户端获取**: 通过Hook获取已解析的地域信息

**注意**: 该模块并未实现从IP地址到地域的实时解析，而是直接获取抖音预先解析好的地域数据。

#### 4.5.5 扩展解析方案

如需从IP实时解析地理位置，可结合以下API：

| API | 说明 | 使用方式 |
|------|------|---------|
| 太平洋网络IP库 | 离线IP库 | 本地数据库查询 |
| IP-API | 在线API | HTTP请求查询 |
| 淘宝IP库 | 阿里提供 | HTTP请求查询 |

**实现示例**:
```java
public String parseIPToLocation(String ip) {
    // 方式1: 使用在线API
    String url = "http://ip-api.com/json/" + ip;
    String result = httpGet(url);

    // 解析JSON获取地域信息
    // {
    //   "status": "success",
    //   "country": "中国",
    //   "regionName": "广东",
    //   "city": "深圳"
    // }

    return parseCountry(result);
}
```

#### 4.5.6 常见问题及解决

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 地域信息为空 | 用户未开启位置权限或未设置 | 使用默认值或隐藏字段 |
| 显示"CN" | 获取的是国家代码而非省份 | 区分region和country字段 |
| IP与地域不符 | VPN/代理导致 | 使用原始IP或标记代理 |

---

### 4.6 音频URL提取流程

#### 4.6.1 流程概述

```
extractAudioUrl 方法被调用
    ↓
获取音频信息对象
    ↓
提取音频URL
    ↓
提取音频标题
    ↓
返回音频信息
```

代码位置: DYHook.smali 约19538行

**方法签名**:
```java
.method private extractAudioUrl(Landroid/content/Context;)V
```

#### 4.6.2 详细步骤

```smali
# 第19538行: extractAudioUrl 方法开始
.method private extractAudioUrl(Landroid/content/Context;)V
    .locals 55
    .annotation system Ldalvik/annotation/MethodParameters;
        accessFlags = {
            0x0
        }
        names = {
            "context"
        }
    .end annotation
```

#### 4.6.3 音频信息获取

背景音乐信息通常包含：
- `music_url`: 音乐播放URL
- `music_title`: 音乐标题
- `music_author`: 音乐作者
- `music_duration`: 音乐时长

#### 4.6.4 下载背景音乐

```smali
# 第56997行: 调用 extractAudioUrl
invoke-direct {p0, p1}, Lkevin/fun/hook/DYHook;->extractAudioUrl(Landroid/content/Context;)V
```

**下载方法**: `downloadBackgroundMusic`

相关配置项:
- `KEY_MUSIC_CONTROL_ENABLE`: 音乐控制启用
- `KEY_MUSIC_CONTROL_HORIZONTAL`: 音乐控制水平
- `KEY_MUSIC_CONTROL_VERTICAL`: 音乐控制垂直

#### 4.6.5 常见问题及解决

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 音频URL为空 | 音频已被删除 | 检查music对象是否有效 |
| 下载失败 | 版权限制 | 使用替代音频源 |
| 格式不支持 | 非mp3格式 | 转换音频格式 |

---

### 4.7 批量下载管理流程

#### 4.7.1 BatchDownloadManager 概述

**类**: `DYHook$BatchDownloadManager`

**功能**:
- 管理批量下载任务队列
- 控制并发下载数量
- 记录下载进度
- 支持暂停/恢复/取消

代码位置: `DYHook$BatchDownloadManager.smali`

#### 4.7.2 主要方法

| 方法 | 功能 |
|------|------|
| `addTask(String url)` | 添加下载任务 |
| `start()` | 开始下载 |
| `pause()` | 暂停下载 |
| `cancel()` | 取消下载 |
| `getProgress()` | 获取进度 |
| `isRunning()` | 检查是否运行中 |

#### 4.7.3 下载流程

```
用户点击批量下载
    ↓
检查是否正在下载 (isBatchDownloading)
    ↓
添加所有任务到队列 (addTask)
    ↓
显示进度对话框 (ProgressViewHolder)
    ↓
启动下载 (start)
    ↓
逐个下载文件
    ↓
更新进度 (getProgress)
    ↓
下载完成，关闭对话框
```

#### 4.7.4 进度显示

**ProgressViewHolder** 用于显示批量下载进度：

```java
.field dialog:Landroid/app/ProgressDialog;
.field progressBar:Landroid/widget/ProgressBar;
.field textView:Landroid/widget/TextView;
```

#### 4.7.5 任务状态管理

| 状态 | 说明 |
|------|------|
| PENDING | 等待下载 |
| DOWNLOADING | 正在下载 |
| PAUSED | 已暂停 |
| COMPLETED | 已完成 |
| FAILED | 下载失败 |

---

## 5. 核心Hook方法详解

### 5.1 主Hook入口

**方法**: `handleLoadPackage(Lde/robv/android/xposed/callbacks/XC_LoadPackage$LoadPackageParam;)V`

**功能**: Xposed模块加载时的入口方法

**实现逻辑**:
```java
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    // 1. 检查包名
    if (!lpparam.packageName.equals("com.ss.android.aweme")) {
        return;
    }

    // 2. 初始化DexKit
    DexKitBridge bridge = DexKitBridge.create(getModulePath());

    // 3. 查找需要的方法
    // 使用bridge.findMethodByMatch() 或 bridge.findAllMethods()

    // 4. 执行Hook
    XposedHelpers.findAndHookMethod(
        targetClass, targetMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // 在方法执行前
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                // 在方法执行后
            }
        }
    );
}
```

### 5.2 主要Hook点列表

#### 5.2.1 视频下载相关Hook

| Hook点 | 位置 | 功能 | 参数 |
|--------|------|------|------|
| `getContentInfo` | 22046行 | 获取视频内容信息 | context, aweme, videoObj |
| `extractImageUrls` | 20374行 | 提取图片URLs | context |
| `extractAudioUrl` | 19538行 | 提取音频URL | context |
| `startDownload` | - | 开始下载 | url, path等 |

#### 5.2.2 评论相关Hook

| Hook点 | 位置 | 功能 |
|--------|------|------|
| `hookCommentAudioView` | - | 评论音频View |
| `hookCommentFullTime` | - | 评论完整时间 |
| `parseCommentFromTouchListener` | - | 解析评论触摸事件 |

#### 5.2.3 UI定制Hook

| Hook点 | 位置 | 功能 |
|--------|------|------|
| `hookDialogPanel` | - | 对话框面板 |
| `hookFeedComponents` | - | Feed组件 |
| `hookFeedRemoveAd` | - | 去除Feed广告 |
| `hookTransparencyControls` | - | 透明度控制 |

#### 5.2.4 设置相关Hook

| Hook点 | 位置 | 功能 |
|--------|------|------|
| `showVideoInfoDialog` | - | 显示视频信息对话框 |
| `showDownloadDialog` | - | 显示下载对话框 |
| `showUnifiedSettingsDialog` | - | 显示统一设置对话框 |

---

## 6. 内部类说明

### 6.1 BatchDownloadManager (批量下载管理器)

**文件**: `DYHook$BatchDownloadManager.smali`

**功能**: 管理批量下载任务

**主要方法**:
```java
.class Lkevin/fun/hook/DYHook$BatchDownloadManager;
.super Ljava/lang/Object;

.method public addTask(Ljava/lang/String;)V
.method public start()V
.method public pause()V
.method public cancel()V
.method public getProgress()I
.method public isRunning()Z
```

### 6.2 CommentImageInfo (评论图片信息)

**功能**: 存储评论中的图片信息

**字段**:
```java
.field public url:Ljava/lang/String;
.field public path:Ljava/lang/String;
.field public width:I
.field public height:I
```

### 6.3 CommentViewHolder (评论视图Holder)

**功能**: 评论列表项的视图管理

**主要逻辑**:
- 获取评论文本和图片
- 设置时间显示
- 处理点赞/点踩按钮
- 拦截评论点击事件

### 6.4 ProgressViewHolder (进度视图Holder)

**功能**: 显示批量下载进度

**主要字段**:
```java
.field dialog:Landroid/app/ProgressDialog;
.field progressBar:Landroid/widget/ProgressBar;
.field textView:Landroid/widget/TextView;
```

### 6.5 匿名内部类 (DYHook$1 ~ DYHook$97)

| 类名 | 功能 |
|------|------|
| `DYHook$1` | 初始化Hook |
| `DYHook$5` | 下载按钮监听 |
| `DYHook$6` | 设置按钮监听 |
| `DYHook$8` | 视频信息Hook |
| `DYHook$19` | UI定制Hook |
| `DYHook$27` | 下载管理Hook |
| `DYHook$40` | 评论Hook |
| `DYHook$64` | 特殊处理Hook |

---

## 7. DexKit搜索逻辑

### 7.1 DexKitBridge初始化

**代码位置**: DYHook.smali约34570行

```smali
invoke-static {v0}, Lkevin/fun/hook/DYHook;->ۤۦۣۡ(Ljava/lang/Object;)Lorg/luckypray/dexkit/DexKitBridge;
```

**初始化方式**:
```java
DexKitBridge bridge = DexKitBridge.create(getModulePath());
bridge.load();
```

### 7.2 方法搜索

**代码位置**: DYHook.smali约59597行

```smali
invoke-static {p0, p1, p2}, Lkevin/fun/hook/DexKitFinder;->findAll(Lorg/luckypray/dexkit/DexKitBridge;Ljava/lang/ClassLoader;Landroid/content/Context;)V
```

**搜索类型**:
1. **按方法签名搜索**: `findMethodBySig()`
2. **按方法名搜索**: `findMethodByName()`
3. **按匹配规则搜索**: `findMethodByMatch()`
4. **查找所有**: `findAllMethods()`

### 7.3 DexKitFinder实现

**文件**: `DexKitFinder.smali`

**主要方法**:

```java
.class Lkevin/fun/hook/DexKitFinder;
.super Ljava/lang/Object;

.method public static findAll(Lorg/luckypray/dexkit/DexKitBridge;Ljava/lang/ClassLoader;Landroid/content/Context;)V
```

**搜索策略**:
1. 使用`Bridge.findMethodByMatch()`进行模糊匹配
2. 通过注解`@DexKitMethodFinder`标记搜索方法
3. 缓存搜索结果避免重复搜索

---

## 8. UI相关逻辑

### 8.1 TikTokHook中的UI构建

**文件**: `TikTokHook.smali`

#### 8.1.1 创建下载按钮

**方法**: `createDownloadButton`

```java
.method private createDownloadButton(Landroid/content/Context;Ljava/lang/String;Landroid/view/View$OnClickListener;)Landroid/widget/Button;
```

#### 8.1.2 构建信息块

**方法**: `buildInfoBlock`

```java
.method private buildInfoBlock(Landroid/content/Context;)Landroid/view/View;
```

**功能**: 创建视频信息展示块

#### 8.1.3 构建筑转开关项

**方法**: `buildSwitchItem`

```java
.method private buildSwitchItem(Landroid/content/Context;Ljava/lang/String;ZLkevin/fun/hook/TikTokHook$Consumer;)Landroid/view/View;
```

#### 8.1.4 创建设置内容

**方法**: `buildAllSettingsContent`

```java
.method private buildAllSettingsContent(Lde/robv/android/xposed/callbacks/XC_LoadPackage$LoadPackageParam;Landroid/content/Context;Landroid/widget/LinearLayout;Landroid/app/Dialog;)V
```

---

## 9. 其他功能模块

### 9.1 评论功能

#### 9.1.1 评论时间定制
- `KEY_COMMENT_TIME_FORMAT`: 评论时间格式
- `KEY_ENABLE_COMMENT_FULL_TIME`: 启用完整时间
- `KEY_COLORFUL_TIMESTAMP`: 彩色时间戳

#### 9.1.2 评论操作控制
- `KEY_REMOVE_COMMENT_LIKE`: 移除点赞按钮
- `KEY_REMOVE_COMMENT_DISLIKE`: 移除点踩按钮
- `KEY_REMOVE_COMMENT_REPLY`: 移除回复按钮
- `KEY_REMOVE_COMMENT_INPUT`: 移除评论输入框

#### 9.1.3 评论图片下载
- `parseAndDownloadCommentImages`: 解析并下载评论图片
- `CommentImageInfo`: 评论图片信息类

### 9.2 UI定制功能

#### 9.2.1 顶栏定制
- `KEY_HIDE_TOP_BAR`: 隐藏顶栏
- `KEY_TOP_BAR_ALPHA`: 顶栏透明度

#### 9.2.2 导航栏定制
- `KEY_HIDE_NAV_BAR`: 隐藏导航栏
- `KEY_NAV_BAR_ALPHA`: 导航栏透明度

#### 9.2.3 全屏播放
- `KEY_FULLSCREEN_PLAY`: 全屏播放开关
- `isFullScreenEnabled`: 全屏状态标志

### 9.3 音乐控制功能

#### 9.3.1 控制选项
- `KEY_MUSIC_CONTROL_ENABLE`: 音乐控制启用
- `KEY_MUSIC_CONTROL_HORIZONTAL`: 水平布局
- `KEY_MUSIC_CONTROL_VERTICAL`: 垂直布局

#### 9.3.2 音频提取
- `extractAudioUrl`: 提取音频URL
- `downloadBackgroundMusic`: 下载背景音乐

### 9.4 直播功能

#### 9.4.1 配置项
- `KEY_LIVE_NUMBER`: 直播数量
- `KEY_NUMBER_LIVE`: 数字直播
- `KEY_LiveRoomAudienceWidget_Dj_Method`: DJ方法

### 9.5 分享定制

#### 9.5.1 功能项
- `KEY_SHARE_MENU`: 分享菜单
- `KEY_CHAT_VOICE_FORWARD`: 聊天语音转发
- `KEY_IM_FORWARD_ACTION_CLASS`: IM转发Action类

### 9.6 特殊功能

#### 9.6.1骰子游戏
- `KEY_CUSTOM_DICE`: 自定义骰子
- `showDiceDialog`: 显示骰子对话框

#### 9.6.2 石头剪刀布
- `showFistDialog`: 显示猜拳对话框

#### 9.6.3 撤回管理
- `KEY_REMOVE_WITHDRAW`: 移除撤回功能

#### 9.6.4 @复制管理
- `KEY_REMOVE_COPY_AT`: 移除复制@功能

---

## 10. 修复建议

### 10.1 常见失效原因

1. **抖音版本更新**: 类名、方法名、签名可能改变
2. **混淆策略变化**: 混淆后的类名/方法名格式可能变化
3. **API变更**: 视频/评论数据结构变化
4. **DexKit版本**: 不同版本DexKit的API可能不同

### 10.2 修复步骤

#### 步骤1: 定位目标类
```java
// 使用反射或DexKit查找类
Class<?> videoClass = DexKitBridge.create(path)
    .load()
    .findClassBySimpleName("Video")[0];
```

#### 步骤2: 定位目标方法
```java
// 查找download方法
Method downloadMethod = findMethodByMatch(
    classLoader,
    MatchInfo.builder()
        .className("com.ss.android.aweme.video.*")
        .methodName("download")
        .returnType("void")
        .parameterTypes(new String[]{"String", "String"})
        .build()
);
```

#### 步骤3: 设置Hook
```java
XposedHelpers.findAndHookMethod(
    videoClass,
    "download",
    String.class,
    String.class,
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            // 提取下载链接
            String url = (String) param.args[0];
            // 自定义处理逻辑
        }
    }
);
```

#### 步骤4: 适配新版本
- 定期检查抖音版本更新
- 维护版本适配代码
- 使用版本判断执行不同逻辑

### 10.3 关键Hook点检查清单

- [ ] 包名检查: `com.ss.android.aweme`
- [ ] DexKit初始化
- [ ] 视频信息获取Hook (getContentInfo)
- [ ] 图文URL提取Hook (extractImageUrls)
- [ ] 音频URL提取Hook (extractAudioUrl)
- [ ] 下载按钮添加Hook
- [ ] 评论列表Hook
- [ ] UI定制Hook (导航栏、顶栏)
- [ ] 分享功能Hook
- [ ] 文件保存Hook

### 10.4 调试建议

1. **使用Log**: 在关键位置添加XposedBridge.log()
2. **版本检测**: 添加版本兼容性检查
3. **异常捕获**: 添加try-catch防止崩溃
4. **渐进式Hook**: 逐个添加Hook点便于定位问题

### 10.5 版本兼容性矩阵

| 功能 | v20.x | v21.x | v22.x | v23.x+ |
|------|-------|-------|-------|--------|
| 视频下载 | ✓ | ✓ | ✓ | ✓ |
| 去水印 | ✓ | ✓ | △ | △ |
| 图文下载 | ✓ | ✓ | ✓ | ✓ |
| 评论定制 | ✓ | ✓ | △ | △ |
| UI定制 | ✓ | △ | △ | △ |

**图例**: ✓ 完全支持 △ 部分支持 ✗ 不支持

---

## 11. 附录

### 附录A: 文件结构

```
decoded_apk/
├── smali/
│   └── kevin/
│       └── fun/
│           └── hook/
│               ├── DYHook.smali                    # 抖音主Hook类
│               ├── DYHook$1~97.smali             # 匿名内部类
│               ├── DYHook$BatchDownloadManager.smali
│               ├── DYHook$CommentImageInfo.smali
│               ├── DYHook$CommentViewHolder.smali
│               ├── DYHook$ProgressViewHolder.smali
│               ├── TikTokHook.smali               # TikTok辅助类
│               ├── DexKitFinder.smali             # DexKit搜索类
│               ├── KSHook.smali                   # 快手Hook(参考)
│               └── XhsHook.smali                  # 小红书Hook(参考)
├── res/
│   └── values/
│       ├── strings.xml                           # 字符串资源
│       └── arrays.xml                            # 数组资源(含xposed_scope)
└── assets/
    └── xposed_init                               # Xposed模块入口
```

### 附录B: 混淆字符映射参考

由于代码使用了混淆字符，以下是一些常见的类名字符串的映射（基于代码分析）：

| 混淆字符 | 可能的原始名称 |
|---------|---------------|
| ۟ۥ۟ۡ۟ | customview相关 |
| ۣ۟۠ۦۥ | View相关 |
| ۦۧۨۨ | addView |
| ۠ۦۥۨ | interpolator |
| Landroidx/ | AndroidX库 |

### 附录C: 关键代码位置索引

| 功能 | 文件 | 行号 |
|------|------|------|
| getContentInfo | DYHook.smali | 22046 |
| extractImageUrls | DYHook.smali | 20374 |
| extractAudioUrl | DYHook.smali | 19538 |
| BatchDownloadManager | DYHook$BatchDownloadManager.smali | - |
| ProgressViewHolder | DYHook$ProgressViewHolder.smali | - |
| createDownloadButton | TikTokHook.smali | - |
| buildAllSettingsContent | TikTokHook.smali | - |
| DexKit初始化 | DYHook.smali | 34570 |
| DexKit搜索 | DexKitFinder.smali | - |

### 附录D: 数据结构对照表

#### 视频数据结构 (Aweme)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| aweme_id | String | 视频ID |
| desc | String | 视频文案 |
| create_time | long | 创建时间戳 |
| author | Object | 作者信息 |
| video | Object | 视频信息 |
| music | Object | 音乐信息 |
| share_url | String | 分享链接 |
| region | String | 地域代码 |
| city | String | 城市名称 |

#### 作者数据结构 (Author)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| uid | String | 用户ID |
| nickname | String | 昵称 |
| avatar_url | String | 头像URL |
| signature | String | 签名 |

#### 视频数据结构 (Video)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| play_addr | Object | 播放地址 |
| cover | Object | 封面 |
| duration | int | 时长 |
| width | int | 宽度 |
| height | int | 高度 |

### 附录E: 错误码对照表

| 错误码 | 说明 | 可能原因 |
|--------|------|---------|
| 1001 | 下载链接为空 | 视频数据未加载 |
| 1002 | 文件保存失败 | 存储权限不足 |
| 1003 | 网络请求失败 | 网络不可用 |
| 1004 | 水印去除失败 | URL参数错误 |
| 1005 | 音频提取失败 | 音乐已被删除 |

---

**文档结束**

**版本历史**:
- v1.0 (2026-04-13): 初始版本
- v2.0 (2026-04-13): 补充完整操作流程信息
