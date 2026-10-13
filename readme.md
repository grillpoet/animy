\# animy



A lightweight Android drawing and animation app. Frame-by-frame animation and paint mode, with layers, selections, and export to image / GIF / MP4.



Features:

\- Frame animation and single-image paint mode

\- Layers with reorder, alpha, visibility, and merge-down

\- Brush types: pen, marker, highlighter, airbrush, eraser

\- Selections: magic wand, lasso, rectangle, ellipse

\- Liquify tool with adjustable brush and strength

\- Onion skin with configurable range

\- Undo / redo via command stack

\- Export: PNG / JPG / WebP, animated GIF, MP4



Tech:

\- Kotlin + Jetpack Compose

\- kotlinx.serialization for project files

\- MediaCodec for MP4, Glide GifEncoder for GIF

\- Immutable domain model with command-based undo

\- Timber for logging



Build:



`./gradlew assembleDebug`



\---



\# animy



一个轻量的 Android 绘画与动画应用。支持逐帧动画和绘画模式，带有图层、选区，可导出为图片 / GIF / MP4。



功能：

\- 帧动画与单图绘画两种模式

\- 图层支持排序、透明度、显隐、向下合并

\- 笔刷：画笔、马克笔、荧光笔、喷枪、橡皮

\- 选区：魔棒、套索、矩形、椭圆

\- 液化工具，可调笔刷大小与强度

\- 洋葱皮，可自定义前后帧范围

\- 命令栈实现撤销 / 重做

\- 导出：PNG / JPG / WebP、动图 GIF、MP4



技术栈：

\- Kotlin + Jetpack Compose

\- kotlinx.serialization 保存项目文件

\- MediaCodec 导出 MP4，Glide GifEncoder 导出 GIF

\- 不可变领域模型 + 命令模式撤销

\- Timber 日志



构建：



`./gradlew assembleDebug`

