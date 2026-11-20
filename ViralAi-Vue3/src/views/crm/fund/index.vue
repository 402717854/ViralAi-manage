<template>
  <div class="app-container fund-page">
    <!-- 顶部汇总 -->
    <el-row :gutter="12" class="summary-row">
      <el-col :span="6">
        <div class="summary-card">
          <span class="summary-label">客户数</span>
          <span class="summary-value">{{ summary.customerCount || 0 }}</span>
          <span class="summary-sub">含零余额客户</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card">
          <span class="summary-label">余额合计</span>
          <span class="summary-value c-primary" :title="full(summary.totalBalance)">{{ money(summary.totalBalance) }}</span>
          <span class="summary-sub">全部客户账户余额之和</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card">
          <span class="summary-label">本月充值</span>
          <span class="summary-value c-success">{{ money(summary.monthRecharge) }}</span>
          <span class="summary-sub">已审核入账的充值</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card">
          <span class="summary-label">本月扣减</span>
          <span class="summary-value c-danger">{{ money(summary.monthDeduct) }}</span>
          <span class="summary-sub">待审核充值 {{ summary.pendingRechargeCount || 0 }} 笔</span>
        </div>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 客户账户 -->
      <el-tab-pane label="客户账户" name="account">
        <el-form :model="accountQuery" ref="accountRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="客户名称" prop="customerName">
            <el-input
              v-model="accountQuery.customerName"
              placeholder="请输入客户名称"
              clearable
              style="width: 200px"
              @keyup.enter="handleAccountQuery"
            />
          </el-form-item>
          <el-form-item label="余额" prop="hasBalance">
            <el-select v-model="accountQuery.hasBalance" placeholder="全部" clearable style="width: 140px">
              <el-option label="有余额" :value="1" />
              <el-option label="零余额" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleAccountQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetAccountQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="openRecharge(null)" v-hasPermi="['crm:fund:recharge']">
              充值登记
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button plain icon="Refresh" @click="loadAll">刷新</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button plain icon="CircleCheck" @click="handleCheckFund">账实校验</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getAccounts" />
        </el-row>

        <el-table v-loading="accountLoading" :data="accountList">
          <el-table-column label="客户名称" prop="customerName" min-width="180" fixed="left" show-overflow-tooltip>
            <template #default="scope">
              <el-link type="primary" :underline="false" @click="openDetail(scope.row)">{{ scope.row.customerName }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="负责人" prop="ownerName" width="110" show-overflow-tooltip />
          <el-table-column label="可用余额" align="right" width="150">
            <template #default="scope">
              <span class="amount-strong" :title="full(scope.row.balance)">{{ money(scope.row.balance) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="累计充值" align="right" width="140">
            <template #default="scope">
              <span :title="full(scope.row.totalRecharge)">{{ money(scope.row.totalRecharge) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="累计扣减" align="right" width="140">
            <template #default="scope">
              <span :title="full(scope.row.totalDeduct)">{{ money(scope.row.totalDeduct) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="累计退款" align="right" width="140">
            <template #default="scope">
              <span :title="full(scope.row.totalRefund)">{{ money(scope.row.totalRefund) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="最近变动" align="center" width="165">
            <template #default="scope">
              <span>{{ parseTime(scope.row.lastFlowTime, '{y}-{m}-{d} {h}:{i}') || "-" }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="240" fixed="right">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="openDetail(scope.row)">流水</el-button>
              <el-button
                link
                type="primary"
                icon="Plus"
                @click="openRecharge(scope.row)"
                v-hasPermi="['crm:fund:recharge']"
              >充值</el-button>
              <el-button
                link
                type="warning"
                icon="RefreshLeft"
                @click="openRefund(scope.row)"
                v-hasPermi="['crm:fund:refund']"
              >退款</el-button>
              <el-button
                link
                type="info"
                icon="EditPen"
                @click="openAdjust(scope.row)"
                v-hasPermi="['crm:fund:adjust']"
              >调账</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="accountTotal > 0"
          :total="accountTotal"
          v-model:page="accountQuery.pageNum"
          v-model:limit="accountQuery.pageSize"
          @pagination="getAccounts"
        />
      </el-tab-pane>

      <!-- 充值单 -->
      <el-tab-pane label="充值单" name="recharge" lazy>
        <el-form :model="rechargeQuery" :inline="true" label-width="80px">
          <el-form-item label="客户名称">
            <el-input
              v-model="rechargeQuery.customerName"
              placeholder="请输入客户名称"
              clearable
              style="width: 180px"
              @keyup.enter="handleRechargeQuery"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="rechargeQuery.status" placeholder="全部" clearable style="width: 140px">
              <el-option label="待审核" :value="0" />
              <el-option label="已入账" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleRechargeQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetRechargeQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="rechargeLoading" :data="rechargeList">
          <el-table-column label="充值单号" prop="rechargeNo" width="200" show-overflow-tooltip />
          <el-table-column label="客户" prop="customerName" min-width="160" show-overflow-tooltip />
          <el-table-column label="充值金额" align="right" width="140">
            <template #default="scope">
              <span class="amount-strong">{{ money(scope.row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="付款方式" prop="payTypeName" width="110" />
          <el-table-column label="付款方" prop="payerName" min-width="140" show-overflow-tooltip />
          <el-table-column label="到账时间" align="center" width="165">
            <template #default="scope">
              <span>{{ parseTime(scope.row.payTime, '{y}-{m}-{d} {h}:{i}') || "-" }}</span>
            </template>
          </el-table-column>
          <el-table-column label="银行流水号" prop="bankSerialNo" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" align="center" width="100">
            <template #default="scope">
              <el-tag :type="rechargeTagType(scope.row.status)">{{ scope.row.statusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="登记人" prop="creatorName" width="100" />
          <el-table-column label="操作" align="center" width="140" fixed="right">
            <template #default="scope">
              <el-button
                v-if="Number(scope.row.status) === 0"
                link
                type="success"
                icon="Stamp"
                @click="openRechargeAudit(scope.row)"
                v-hasPermi="['crm:fund:recharge-audit']"
              >审核</el-button>
              <span v-else class="plain-text">{{ scope.row.auditorName || "-" }}</span>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="rechargeTotal > 0"
          :total="rechargeTotal"
          v-model:page="rechargeQuery.pageNum"
          v-model:limit="rechargeQuery.pageSize"
          @pagination="getRecharges"
        />
      </el-tab-pane>

      <!-- 退款单 -->
      <el-tab-pane label="退款单" name="refund" lazy>
        <el-form :model="refundQuery" :inline="true" label-width="80px">
          <el-form-item label="客户名称">
            <el-input
              v-model="refundQuery.customerName"
              placeholder="请输入客户名称"
              clearable
              style="width: 180px"
              @keyup.enter="handleRefundQuery"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="refundQuery.status" placeholder="全部" clearable style="width: 140px">
              <el-option label="待审批" :value="0" />
              <el-option label="已退款" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleRefundQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetRefundQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="refundLoading" :data="refundList">
          <el-table-column label="退款单号" prop="refundNo" width="200" show-overflow-tooltip />
          <el-table-column label="客户" prop="customerName" min-width="160" show-overflow-tooltip />
          <el-table-column label="退款金额" align="right" width="140">
            <template #default="scope">
              <span class="amount-strong c-danger">{{ money(scope.row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="退款原因" prop="reason" min-width="200" show-overflow-tooltip />
          <el-table-column label="收款户名" prop="receiveName" width="140" show-overflow-tooltip />
          <el-table-column label="状态" align="center" width="100">
            <template #default="scope">
              <el-tag :type="refundTagType(scope.row.status)">{{ scope.row.statusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" align="center" width="165">
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') || "-" }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="140" fixed="right">
            <template #default="scope">
              <el-button
                v-if="Number(scope.row.status) === 0"
                link
                type="success"
                icon="Stamp"
                @click="openRefundAudit(scope.row)"
                v-hasPermi="['crm:fund:refund-audit']"
              >审批</el-button>
              <span v-else class="plain-text">{{ scope.row.auditorName || "-" }}</span>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="refundTotal > 0"
          :total="refundTotal"
          v-model:page="refundQuery.pageNum"
          v-model:limit="refundQuery.pageSize"
          @pagination="getRefunds"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 账户详情 + 流水 -->
    <el-drawer v-model="detailOpen" title="账户详情" size="60%" append-to-body>
      <div v-if="detailAccount" v-loading="detailLoading">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="客户">{{ detailAccount.customerName }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detailAccount.ownerName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="可用余额">
            <span class="amount-strong">{{ money(detailAccount.balance) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="累计充值">{{ money(detailAccount.totalRecharge) }}</el-descriptions-item>
          <el-descriptions-item label="累计扣减">{{ money(detailAccount.totalDeduct) }}</el-descriptions-item>
          <el-descriptions-item label="累计退款">{{ money(detailAccount.totalRefund) }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-toolbar">
          <el-select v-model="flowQuery.bizType" placeholder="全部类型" clearable style="width: 160px" @change="handleFlowQuery">
            <el-option v-for="item in BIZ_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button icon="Refresh" @click="getFlows">刷新</el-button>
        </div>

        <el-table v-loading="flowLoading" :data="flowList" size="small" max-height="420">
          <el-table-column label="流水号" prop="flowNo" width="190" show-overflow-tooltip />
          <el-table-column label="类型" prop="bizTypeName" width="100" />
          <el-table-column label="金额" align="right" width="130">
            <template #default="scope">
              <span :class="Number(scope.row.direction) === 1 ? 'c-success' : 'c-danger'">
                {{ Number(scope.row.direction) === 1 ? "+" : "-" }}{{ money(scope.row.amount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="变动后余额" align="right" width="140">
            <template #default="scope">{{ money(scope.row.balanceAfter) }}</template>
          </el-table-column>
          <el-table-column label="关联单号" prop="bizNo" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作人" prop="operatorName" width="100" />
          <el-table-column label="时间" align="center" width="160">
            <template #default="scope">
              <span>{{ parseTime(scope.row.flowTime, '{y}-{m}-{d} {h}:{i}') || "-" }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
        </el-table>

        <pagination
          v-show="flowTotal > 0"
          :total="flowTotal"
          v-model:page="flowQuery.pageNum"
          v-model:limit="flowQuery.pageSize"
          @pagination="getFlows"
        />
      </div>
    </el-drawer>

    <!-- 充值登记 -->
    <el-dialog title="充值登记" v-model="rechargeOpen" width="580px" append-to-body>
      <el-form :model="rechargeForm" :rules="rechargeRules" ref="rechargeFormRef" label-width="100px">
        <el-form-item label="客户" prop="customerId">
          <el-select
            v-model="rechargeForm.customerId"
            placeholder="请选择客户"
            filterable
            :disabled="!!rechargeForm.customerLocked"
            style="width: 100%"
          >
            <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="充值金额" prop="amount">
          <el-input v-model="rechargeForm.amount" placeholder="请输入充值金额，最多 8 位小数" />
        </el-form-item>
        <el-form-item label="付款方式" prop="payType">
          <el-select v-model="rechargeForm.payType" style="width: 100%">
            <el-option label="对公转账" :value="1" />
            <el-option label="在线支付" :value="2" />
            <el-option label="现金" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款方">
          <el-input v-model="rechargeForm.payerName" placeholder="可与客户名称不同（代付）" maxlength="200" />
        </el-form-item>
        <el-form-item label="到账时间">
          <el-date-picker
            v-model="rechargeForm.payTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择到账时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="银行流水号" prop="bankSerialNo">
          <el-input v-model="rechargeForm.bankSerialNo" placeholder="同一客户下唯一，用于防止重复登记" maxlength="128" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="2" maxlength="480" show-word-limit />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="登记后不会立即入账，需财务审核通过才计入客户余额"
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="rechargeSubmitting" @click="submitRecharge">提交</el-button>
          <el-button @click="rechargeOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 退款申请 -->
    <el-dialog title="退款申请" v-model="refundOpen" width="580px" append-to-body>
      <el-form :model="refundForm" :rules="refundRules" ref="refundFormRef" label-width="100px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="refundForm.customerId" placeholder="请选择客户" filterable style="width: 100%">
            <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="可用余额">
          <span class="amount-strong">{{ money(refundAvailable) }}</span>
        </el-form-item>
        <el-form-item label="退款金额" prop="amount">
          <el-input v-model="refundForm.amount" placeholder="不得超过可用余额" />
        </el-form-item>
        <el-form-item label="退款方式" prop="refundType">
          <el-radio-group v-model="refundForm.refundType">
            <el-radio :value="1">原路退回</el-radio>
            <el-radio :value="2">其他方式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="收款户名">
          <el-input v-model="refundForm.receiveName" maxlength="200" />
        </el-form-item>
        <el-form-item label="收款账号">
          <el-input v-model="refundForm.receiveAccount" maxlength="64" />
        </el-form-item>
        <el-form-item label="开户行">
          <el-input v-model="refundForm.receiveBank" maxlength="200" />
        </el-form-item>
        <el-form-item label="退款原因" prop="reason">
          <el-input v-model="refundForm.reason" type="textarea" :rows="2" maxlength="480" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交</el-button>
          <el-button @click="refundOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 人工调账 -->
    <el-dialog title="人工调账" v-model="adjustOpen" width="520px" append-to-body>
      <el-form :model="adjustForm" :rules="adjustRules" ref="adjustFormRef" label-width="100px">
        <el-form-item label="客户">
          <span class="plain-text">{{ adjustForm.customerName }}</span>
        </el-form-item>
        <el-form-item label="当前余额">
          <span class="amount-strong">{{ money(adjustForm.balance) }}</span>
        </el-form-item>
        <el-form-item label="调账方向" prop="direction">
          <el-radio-group v-model="adjustForm.direction">
            <el-radio :value="1">调增</el-radio>
            <el-radio :value="2">调减</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调账金额" prop="amount">
          <el-input v-model="adjustForm.amount" placeholder="请输入金额" />
        </el-form-item>
        <el-form-item label="调账原因" prop="reason">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" maxlength="480" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="adjustSubmitting" @click="submitAdjust">确定</el-button>
          <el-button @click="adjustOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 充值 / 退款审核 -->
    <el-dialog :title="auditDialog.title" v-model="auditOpen" width="520px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="单号">
          <span class="plain-text">{{ auditDialog.bizNo }}</span>
        </el-form-item>
        <el-form-item label="金额">
          <span class="amount-strong">{{ money(auditDialog.amount) }}</span>
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditDialog.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditDialog.remark" type="textarea" :rows="3" maxlength="480" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="auditSubmitting" @click="submitAudit">确定</el-button>
          <el-button @click="auditOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Fund">
import {
  listFundAccount,
  getFundAccount,
  getFundSummary,
  listFundFlow,
  listFundRecharge,
  createFundRecharge,
  auditFundRecharge,
  listFundRefund,
  createFundRefund,
  auditFundRefund,
  adjustFund,
  checkFund
} from "@/api/crm/fund"
import { getCustomerOptions } from "@/api/crm/customer"
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()

/** 流水类型，与后端 CustomerFundBizTypeEnum 一致 */
const BIZ_TYPES = [
  { value: 1, label: "充值" },
  { value: 2, label: "结算扣款" },
  { value: 3, label: "退款" },
  { value: 4, label: "人工调增" },
  { value: 5, label: "人工调减" },
  { value: 6, label: "冲销退回" },
  { value: 7, label: "期初余额" },
  { value: 8, label: "结算撤销" },
  { value: 9, label: "撤销冲销" }
]

const showSearch = ref(true)
const activeTab = ref("account")
const summary = ref({})

const route = useRoute()
// 首页待办下钻：带 tab 参数直接落到对应页签
if (route.query.tab === "recharge" || route.query.tab === "refund") {
  activeTab.value = String(route.query.tab)
}

const customerOptions = ref([])

/* ---------------- 客户账户 ---------------- */
const accountLoading = ref(false)
const accountList = ref([])
const accountTotal = ref(0)
const accountQuery = ref({ pageNum: 1, pageSize: 10, customerName: undefined, hasBalance: undefined })

/* ---------------- 流水 ---------------- */
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailAccount = ref(null)
const flowLoading = ref(false)
const flowList = ref([])
const flowTotal = ref(0)
const flowQuery = ref({ pageNum: 1, pageSize: 20, customerId: undefined, bizType: undefined })

/* ---------------- 充值单 ---------------- */
const rechargeLoading = ref(false)
const rechargeList = ref([])
const rechargeTotal = ref(0)
const rechargeQuery = ref({ pageNum: 1, pageSize: 10, customerName: undefined, status: undefined })

/* ---------------- 退款单 ---------------- */
const refundLoading = ref(false)
const refundList = ref([])
const refundTotal = ref(0)
const refundQuery = ref({ pageNum: 1, pageSize: 10, customerName: undefined, status: undefined })

/* ---------------- 弹窗 ---------------- */
const rechargeOpen = ref(false)
const rechargeSubmitting = ref(false)
const rechargeFormRef = ref()
const rechargeForm = ref({})
const rechargeRules = {
  customerId: [{ required: true, message: "请选择客户", trigger: "change" }],
  amount: [
    { required: true, message: "请输入充值金额", trigger: "blur" },
    { pattern: /^\d+(\.\d{1,8})?$/, message: "金额格式不正确，最多 8 位小数", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== undefined && Number(value) <= 0) {
          callback(new Error("充值金额必须大于 0"))
        } else {
          callback()
        }
      },
      trigger: "blur"
    }
  ]
}

const refundOpen = ref(false)
const refundSubmitting = ref(false)
const refundFormRef = ref()
const refundForm = ref({})
const refundAvailable = ref(0)
const refundRules = {
  customerId: [{ required: true, message: "请选择客户", trigger: "change" }],
  amount: [
    { required: true, message: "请输入退款金额", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== undefined && Number(value) > Number(refundAvailable.value)) {
          callback(new Error("退款金额不能超过可用余额"))
        } else {
          callback()
        }
      },
      trigger: "blur"
    }
  ],
  reason: [{ required: true, message: "请填写退款原因", trigger: "blur" }]
}

const adjustOpen = ref(false)
const adjustSubmitting = ref(false)
const adjustFormRef = ref()
const adjustForm = ref({})
const adjustRules = {
  direction: [{ required: true, message: "请选择调账方向", trigger: "change" }],
  amount: [{ required: true, message: "请输入调账金额", trigger: "blur" }],
  reason: [{ required: true, message: "请填写调账原因", trigger: "blur" }]
}

const auditOpen = ref(false)
const auditSubmitting = ref(false)
const auditDialog = ref({ type: "recharge", title: "", bizNo: "", amount: 0, id: null, pass: true, remark: "" })

/* ---------------- 展示工具 ---------------- */
function money(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function full(value) {
  if (value === null || value === undefined || value === "") {
    return ""
  }
  return Number(value).toFixed(8)
}

function rechargeTagType(status) {
  const value = Number(status)
  if (value === 0) return "warning"
  if (value === 1) return "success"
  return "info"
}

function refundTagType(status) {
  const value = Number(status)
  if (value === 0) return "warning"
  if (value === 1) return "success"
  return "info"
}

/* ---------------- 数据加载 ---------------- */
function getSummary() {
  return getFundSummary().then(res => {
    summary.value = res.data || {}
  })
}

function getAccounts() {
  accountLoading.value = true
  return listFundAccount(accountQuery.value).then(res => {
    accountList.value = res.rows || []
    accountTotal.value = res.total || 0
  }).finally(() => {
    accountLoading.value = false
  })
}

function handleAccountQuery() {
  accountQuery.value.pageNum = 1
  getAccounts()
}

function resetAccountQuery() {
  accountQuery.value = { pageNum: 1, pageSize: 10, customerName: undefined, hasBalance: undefined }
  getAccounts()
}

function getFlows() {
  flowLoading.value = true
  return listFundFlow(flowQuery.value).then(res => {
    flowList.value = res.rows || []
    flowTotal.value = res.total || 0
  }).finally(() => {
    flowLoading.value = false
  })
}

function handleFlowQuery() {
  flowQuery.value.pageNum = 1
  getFlows()
}

function getRecharges() {
  rechargeLoading.value = true
  return listFundRecharge(rechargeQuery.value).then(res => {
    rechargeList.value = res.rows || []
    rechargeTotal.value = res.total || 0
  }).finally(() => {
    rechargeLoading.value = false
  })
}

function handleRechargeQuery() {
  rechargeQuery.value.pageNum = 1
  getRecharges()
}

function resetRechargeQuery() {
  rechargeQuery.value = { pageNum: 1, pageSize: 10, customerName: undefined, status: undefined }
  getRecharges()
}

function getRefunds() {
  refundLoading.value = true
  return listFundRefund(refundQuery.value).then(res => {
    refundList.value = res.rows || []
    refundTotal.value = res.total || 0
  }).finally(() => {
    refundLoading.value = false
  })
}

function handleRefundQuery() {
  refundQuery.value.pageNum = 1
  getRefunds()
}

function resetRefundQuery() {
  refundQuery.value = { pageNum: 1, pageSize: 10, customerName: undefined, status: undefined }
  getRefunds()
}

function loadAll() {
  getSummary()
  getAccounts()
}

function onTabChange(name) {
  if (name === "recharge" && !rechargeList.value.length) {
    getRecharges()
  }
  if (name === "refund" && !refundList.value.length) {
    getRefunds()
  }
}

function loadCustomerOptions() {
  if (customerOptions.value.length) {
    return
  }
  getCustomerOptions().then(res => {
    customerOptions.value = res.data || []
  })
}

/* ---------------- 账户详情 ---------------- */
function openDetail(row) {
  detailOpen.value = true
  detailLoading.value = true
  detailAccount.value = null
  flowQuery.value = { pageNum: 1, pageSize: 20, customerId: row.customerId, bizType: undefined }
  getFundAccount(row.customerId).then(res => {
    detailAccount.value = res.data || null
    flowList.value = []
    flowTotal.value = 0
    getFlows()
  }).finally(() => {
    detailLoading.value = false
  })
}

/* ---------------- 充值 ---------------- */
function openRecharge(row) {
  loadCustomerOptions()
  rechargeForm.value = {
    customerId: row ? row.customerId : undefined,
    customerLocked: !!row,
    amount: undefined,
    payType: 1,
    payerName: row ? row.customerName : undefined,
    payTime: parseTime(new Date(), "{y}-{m}-{d} {h}:{i}:{s}"),
    bankSerialNo: undefined,
    remark: undefined
  }
  rechargeOpen.value = true
  nextTick(() => rechargeFormRef.value && rechargeFormRef.value.clearValidate())
}

function submitRecharge() {
  rechargeFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    rechargeSubmitting.value = true
    createFundRecharge({
      customerId: rechargeForm.value.customerId,
      amount: rechargeForm.value.amount,
      payType: rechargeForm.value.payType,
      payerName: rechargeForm.value.payerName,
      payTime: rechargeForm.value.payTime,
      bankSerialNo: rechargeForm.value.bankSerialNo,
      remark: rechargeForm.value.remark
    }).then(res => {
      proxy.$modal.msgSuccess(`充值单 ${res.data} 已提交，待财务审核入账`)
      rechargeOpen.value = false
      getSummary()
      if (activeTab.value === "recharge") {
        getRecharges()
      }
    }).finally(() => {
      rechargeSubmitting.value = false
    })
  })
}

/* ---------------- 退款 ---------------- */
function openRefund(row) {
  loadCustomerOptions()
  refundForm.value = {
    customerId: row.customerId,
    amount: undefined,
    refundType: 1,
    receiveName: undefined,
    receiveAccount: undefined,
    receiveBank: undefined,
    reason: undefined
  }
  refundAvailable.value = row.availableBalance === undefined ? row.balance : row.availableBalance
  refundOpen.value = true
  nextTick(() => refundFormRef.value && refundFormRef.value.clearValidate())
}

function submitRefund() {
  refundFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    refundSubmitting.value = true
    createFundRefund({ ...refundForm.value }).then(res => {
      proxy.$modal.msgSuccess(`退款单 ${res.data} 已提交，待审批`)
      refundOpen.value = false
      if (activeTab.value === "refund") {
        getRefunds()
      }
    }).finally(() => {
      refundSubmitting.value = false
    })
  })
}

/* ---------------- 调账 ---------------- */
function openAdjust(row) {
  adjustForm.value = {
    customerId: row.customerId,
    customerName: row.customerName,
    balance: row.balance,
    direction: 1,
    amount: undefined,
    reason: undefined
  }
  adjustOpen.value = true
  nextTick(() => adjustFormRef.value && adjustFormRef.value.clearValidate())
}

function submitAdjust() {
  adjustFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    adjustSubmitting.value = true
    adjustFund({
      customerId: adjustForm.value.customerId,
      amount: adjustForm.value.amount,
      direction: adjustForm.value.direction,
      reason: adjustForm.value.reason
    }).then(res => {
      const flow = res.data || {}
      proxy.$modal.msgSuccess(`调账成功，当前余额 ${money(flow.balanceAfter)}`)
      adjustOpen.value = false
      loadAll()
      if (detailOpen.value && flowQuery.value.customerId === adjustForm.value.customerId) {
        getFlows()
      }
    }).finally(() => {
      adjustSubmitting.value = false
    })
  })
}

/* ---------------- 审核 ---------------- */
function openRechargeAudit(row) {
  auditDialog.value = {
    type: "recharge",
    title: "充值审核",
    id: row.id,
    bizNo: row.rechargeNo,
    amount: row.amount,
    pass: true,
    remark: ""
  }
  auditOpen.value = true
}

function openRefundAudit(row) {
  auditDialog.value = {
    type: "refund",
    title: "退款审批",
    id: row.id,
    bizNo: row.refundNo,
    amount: row.amount,
    pass: true,
    remark: ""
  }
  auditOpen.value = true
}

function submitAudit() {
  const payload = {
    id: auditDialog.value.id,
    pass: auditDialog.value.pass,
    remark: auditDialog.value.remark
  }
  if (!payload.pass && !String(payload.remark || "").trim()) {
    proxy.$modal.msgWarning("驳回时请填写原因")
    return
  }
  auditSubmitting.value = true
  const action = auditDialog.value.type === "recharge" ? auditFundRecharge : auditFundRefund
  action(payload).then(() => {
    proxy.$modal.msgSuccess(payload.pass ? "已通过" : "已驳回")
    auditOpen.value = false
    if (auditDialog.value.type === "recharge") {
      getRecharges()
    } else {
      getRefunds()
    }
    getSummary()
    getAccounts()
  }).finally(() => {
    auditSubmitting.value = false
  })
}

/* ---------------- 账实校验 ---------------- */
function handleCheckFund() {
  checkFund().then(res => {
    const list = res.data || []
    if (!list.length) {
      proxy.$modal.msgSuccess("账实一致，未发现差异")
      return
    }
    const detail = list
      .slice(0, 5)
      .map(item => `${item.customerName}：余额 ${money(item.balance)}，流水 ${money(item.flowSum)}，差额 ${money(item.diff)}`)
      .join("；")
    proxy.$alert(detail, `发现 ${list.length} 个账户账实不一致`, { type: "warning" })
  })
}

loadAll()
loadCustomerOptions()
// lazy 页签不会自动加载，首屏落在充值/退款页签时需主动拉一次
if (activeTab.value === "recharge") {
  getRecharges()
} else if (activeTab.value === "refund") {
  getRefunds()
}
</script>

<style scoped>
.summary-row {
  margin-bottom: 12px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.summary-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.summary-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
}

.summary-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.amount-strong {
  font-weight: 600;
}

.c-primary {
  color: var(--el-color-primary);
}

.c-success {
  color: var(--el-color-success);
}

.c-danger {
  color: var(--el-color-danger);
}

.plain-text {
  color: var(--el-text-color-regular);
}

.mb8 {
  margin-bottom: 8px;
}

.drawer-toolbar {
  display: flex;
  gap: 8px;
  margin: 12px 0 8px;
}
</style>
