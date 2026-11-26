package Zeze.Netty;


/**
 * 请求流结束（或完整请求就绪）的处理回调。
 * <ul>
 *   <li>非流模式：收到完整请求体（{@code LastHttpContent}）后触发。</li>
 *   <li>流模式且已注册 {@link HttpAbortStreamHandle}（包括 {@link HttpMultipartHandle} /
 *       {@link HttpFileUploadHandle}）：仅在正常收齐 {@code LastHttpContent} 时触发，
 *       此时 {@link HttpExchange#isStreamAborted()} 为 {@code false}；流中止改走
 *       {@link HttpAbortStreamHandle#onAbortStream(HttpExchange)}，二者互斥。</li>
 *   <li>流模式但未注册 {@link HttpAbortStreamHandle}（旧三参数流式构造兼容路径）：流中止时
 *       仍会回退触发本回调以便释放资源，此时 {@link HttpExchange#isStreamAborted()} 为 {@code true}，
 *       实现方应先检查该标志，避免将截断或拒收的上传当作完整请求处理。</li>
 * </ul>
 */
@FunctionalInterface
public interface HttpEndStreamHandle {
	void onEndStream(HttpExchange x) throws Exception;
}
