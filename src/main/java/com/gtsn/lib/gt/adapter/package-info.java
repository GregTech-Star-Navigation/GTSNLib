/**
 * GTCEu 访问适配层（ADR-0005）。
 *
 * <h2>包边界纪律（强制）</h2>
 * <p>本包是 GTSNLib 内<b>唯一</b>允许 import GTCEu（{@code com.gregtechceu.gtceu.*}）的地方。
 * 库内其它任何包（{@code api}/{@code core}/{@code integration}/{@code gt.registration}/{@code ui} 等）
 * 一律不得直接引用 GTCEu 类型，必须经 {@link com.gtsn.lib.gt.adapter.GtAdapter} 门面访问。</p>
 *
 * <p>搜索验证（应只命中本包）：</p>
 * <pre>
 *   grep -rn "com.gregtechceu" src/main/java --include=*.java
 * </pre>
 *
 * <h2>为什么</h2>
 * <p>GTCEu 7.5.3 是当前编译目标；上游 8.0 计划移除 {@code GTCEuAPI.materialManager}、
 * {@code MaterialRegistryEvent}、{@code GTRegistrate#registerRegistrate} 等入口。将访问收敛在本包后，
 * 上游破坏性变更变成一次受控的包内迁移（ADR-0005）。</p>
 *
 * <h2>结构</h2>
 * <ul>
 *   <li>{@link com.gtsn.lib.gt.adapter.GtAdapter} — 对外门面，只暴露 GTSN 自有类型。</li>
 *   <li>{@link com.gtsn.lib.gt.adapter.GtBackend} — 可注入后端端口；查询逻辑据此纯单测。</li>
 *   <li>{@link com.gtsn.lib.gt.adapter.GtceBackend} — 唯一直接接触 GTCEu 的生产实现。</li>
 *   <li>{@link com.gtsn.lib.gt.adapter.GtNames} — 纯映射逻辑（归一化 / 资源位置 / tag prefix）。</li>
 *   <li>{@link com.gtsn.lib.gt.adapter.GtRegistrateHandle} — #12/#13 将使用的 GT 注册入口。</li>
 * </ul>
 *
 * <p>本票仅集中“访问”，不实现注册便捷 DSL（#12/#13）。</p>
 */
package com.gtsn.lib.gt.adapter;
