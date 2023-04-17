# TypeScript

首次编译前, 先运行`npm install --save-dev typescript`生成`node_modules`目录和`package-lock.json`文件.
然后运行`node_modules\.bin\tsc.cmd`即可编译, 或者用 Visual Studio 打开`TypeScript.njsproj`再编译.

### 运行 ByteBuffer 测试

先成功编译, 设置环境变量`NODE_PATH=.`, 再运行`node app.js`.
