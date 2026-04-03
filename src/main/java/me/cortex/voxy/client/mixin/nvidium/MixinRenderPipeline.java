package me.cortex.voxy.client.mixin.nvidium;

// [高风险未适配] Nvidium 0.3.1 仍显式依赖 Sodium 0.5.9/0.5.11 的 API，
// 与当前 1.21.1 基线使用的 Sodium 0.6.13 不兼容，故已在 mixins JSON 中禁用。
// 详见 .vscode/MCP/agent-state.json 中对任务 7 的记录。
public final class MixinRenderPipeline {
    private MixinRenderPipeline() {
    }
}
