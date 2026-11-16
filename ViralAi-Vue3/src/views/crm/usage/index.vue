<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 对账台账 -->
      <el-tab-pane label="对账台账" name="usage">
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="流水号" prop="callNo">
            <el-input
              v-model="queryParams.callNo"
              placeholder="请输入流水号"
              clearable
              style="width: 220px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="客户" prop="customerId">
            <el-select
              v-model="queryParams.customerId"
              placeholder="请选择客户"
              filterable
              clearable
              :loading="customerLoading"
              style="width: 180px"
            >
              <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="渠道" prop="supplierId">
            <el-select
              v-model="queryParams.supplierId"
              placeholder="请选择渠道"
              filterable
              clearable
              :loading="supplierLoading"
              style="width: 180px"
            >
              <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型编码" prop="productCode">
            <el-input
              v-model="queryParams.productCode"
              placeholder="请输入模型编码"
              clearable
              style="width: 180px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="匹配状态" prop="matchStatusList">
            <el-select
              v-model="queryParams.matchStatusList"
              placeholder="请选择"
              multiple
              collapse-tags
              clearable
              style="width: 220px"
            >
              <el-option
                v-for="dict in crm_usage_match_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="调用时间">
            <el-date-picker
              v-model="dateRange"
              value-format="YYYY-MM-DD"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="Refresh"
              @click="openReconcile"
              v-hasPermi="['crm:usage:reconcile']"
            >手动对账</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              icon="Download"
              :loading="exportLoading.usage"
              @click="handleExport"
              v-hasPermi="['crm:usage:export']"
            >导出</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="usageList">
          <el-table-column label="流水号" align="center" prop="callNo" min-width="230" fixed="left" show-overflow-tooltip>
            <template #default="scope">
              <el-link type="primary" :underline="false" @click="handleViewUsage(scope.row)">
                {{ scope.row.callNo }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="客户" align="center" prop="customerName" min-width="130" show-overflow-tooltip />
          <el-table-column label="渠道" align="center" prop="supplierName" min-width="120" show-overflow-tooltip />
          <el-table-column label="渠道账户" align="center" min-width="130" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ scope.row.supplierAccountName || (scope.row.supplierAccountId ? scope.row.supplierAccountId : "未归属") }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模型编码" align="center" prop="productCode" min-width="150" show-overflow-tooltip />
          <el-table-column label="token" align="center" prop="totalTokens" width="110" />
          <el-table-column label="刊例价费用" align="center" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.totalFees) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="折后费用" align="center" width="160" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.actualFees) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="应收金额" align="center" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.sellAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成本金额" align="center" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.costAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="毛利" align="center" width="140" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.grossProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="匹配状态" align="center" width="130">
            <template #default="scope">
              <dict-tag :options="crm_usage_match_status" :value="String(scope.row.matchStatus)" />
            </template>
          </el-table-column>
          <el-table-column label="合同编号" align="center" prop="contactNo" min-width="180" show-overflow-tooltip />
          <el-table-column label="账单编号" align="center" prop="billNo" min-width="160" show-overflow-tooltip />
          <el-table-column label="用量类型" align="center" width="100">
            <template #default="scope">
              <dict-tag :options="crm_usage_type" :value="String(scope.row.usageType ?? 0)" />
            </template>
          </el-table-column>
          <el-table-column label="调用时间" align="center" prop="callTime" width="165">
            <template #default="scope">
              <span>{{ parseTime(scope.row.callTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="150" fixed="right">
            <template #default="scope">
              <el-button
                v-if="canRematch(scope.row)"
                link
                type="primary"
                icon="Refresh"
                @click="handleRematch(scope.row)"
                v-hasPermi="['crm:usage:reconcile']"
              >重新匹配</el-button>
              <el-button link type="info" icon="View" @click="handleViewUsage(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </el-tab-pane>

      <!-- 网关原始流水 -->
      <el-tab-pane label="网关原始流水" name="callLog">
        <el-form :model="logQuery" ref="logQueryRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="流水号" prop="callNo">
            <el-input
              v-model="logQuery.callNo"
              placeholder="请输入流水号"
              clearable
              style="width: 220px"
              @keyup.enter="handleLogQuery"
            />
          </el-form-item>
          <el-form-item label="客户" prop="customerId">
            <el-select
              v-model="logQuery.customerId"
              placeholder="请选择客户"
              filterable
              clearable
              style="width: 180px"
            >
              <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="渠道" prop="supplierId">
            <el-select
              v-model="logQuery.supplierId"
              placeholder="请选择渠道"
              filterable
              clearable
              style="width: 180px"
            >
              <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型编码" prop="productCode">
            <el-input
              v-model="logQuery.productCode"
              placeholder="请输入模型编码"
              clearable
              style="width: 180px"
              @keyup.enter="handleLogQuery"
            />
          </el-form-item>
          <el-form-item label="调用状态" prop="callStatus">
            <el-select v-model="logQuery.callStatus" placeholder="全部" clearable style="width: 140px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="调用时间">
            <el-date-picker
              v-model="logDateRange"
              value-format="YYYY-MM-DD"
              type="daterange"
              :clearable="false"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleLogQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetLogQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="Download"
              @click="openPull"
              v-hasPermi="['crm:usage:reconcile']"
            >拉取流水</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="success"
              plain
              icon="Upload"
              @click="openUpload"
              v-hasPermi="['crm:usage:ingest']"
            >上传流水</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              icon="Download"
              :loading="exportLoading.callLog"
              @click="handleLogExport"
              v-hasPermi="['crm:usage:export']"
            >导出流水</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getCallLogList"></right-toolbar>
        </el-row>

        <el-table v-loading="logLoading" :data="callLogList">
          <el-table-column label="流水号" align="center" prop="callNo" min-width="230" fixed="left" show-overflow-tooltip />
          <el-table-column label="客户" align="center" prop="customerName" min-width="130" show-overflow-tooltip />
          <el-table-column label="渠道" align="center" prop="supplierName" min-width="120" show-overflow-tooltip />
          <el-table-column label="渠道账户" align="center" min-width="130" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ scope.row.supplierAccountName || (scope.row.supplierAccountId ? scope.row.supplierAccountId : "未归属") }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模型编码" align="center" prop="productCode" min-width="150" show-overflow-tooltip />
          <el-table-column label="模型类型" align="center" prop="modelType" width="110" />
          <el-table-column label="输入/输出" align="center" width="130">
            <template #default="scope">
              <span>{{ scope.row.promptTokens || 0 }} / {{ scope.row.completionTokens || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="总token" align="center" prop="totalTokens" width="110" />
          <el-table-column label="刊例价费用" align="center" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.totalFees) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="折后费用" align="center" width="160" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.actualFees) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="调用状态" align="center" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.callStatus === 1 ? 'success' : 'danger'">
                {{ scope.row.callStatus === 1 ? "成功" : "失败" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="是否已对账" align="center" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.reconciled === 1 ? 'success' : 'info'">
                {{ scope.row.reconciled === 1 ? "已对账" : "未对账" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="调用时间" align="center" prop="callTime" width="165">
            <template #default="scope">
              <span>{{ parseTime(scope.row.callTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="失败原因" align="center" prop="errMsg" min-width="160" show-overflow-tooltip />
        </el-table>

        <pagination
          v-show="logTotal > 0"
          :total="logTotal"
          v-model:page="logQuery.pageNum"
          v-model:limit="logQuery.pageSize"
          @pagination="getCallLogList"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 手动对账 -->
    <el-dialog title="手动执行对账" v-model="reconcileOpen" width="560px" append-to-body>
      <el-form :model="reconcileForm" :rules="reconcileRules" ref="reconcileRef" label-width="110px">
        <el-form-item label="调用时间" prop="timeRange">
          <el-date-picker
            v-model="reconcileForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="-"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="渠道" prop="supplierId">
          <el-select v-model="reconcileForm.supplierId" placeholder="不选表示当前账号可见的全部渠道" filterable clearable style="width: 100%">
            <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="重试异常记录">
          <el-switch v-model="reconcileForm.retryAbnormal" />
          <span class="form-tip">开启后对「无合同 / 已过期 / 其他异常」的台账重新匹配，补签合同后使用</span>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="对账会扫描区间内未生成台账的网关流水，按客户+渠道+模型匹配生效合同并计费；已生成账单的用量不会被重算。"
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitReconcile" :loading="reconcileSubmitting">开始对账</el-button>
          <el-button @click="reconcileOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 拉取流水 -->
    <el-dialog title="从渠道账户拉取流水" v-model="pullOpen" width="600px" append-to-body>
      <el-form :model="pullForm" :rules="pullRules" ref="pullRef" label-width="110px">
        <el-form-item label="调用时间" prop="timeRange">
          <el-date-picker
            v-model="pullForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="-"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="渠道" prop="supplierId">
          <el-select
            v-model="pullForm.supplierId"
            placeholder="不选表示全部渠道"
            filterable
            clearable
            style="width: 100%"
            @change="handlePullSupplierChange"
          >
            <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道账户" prop="supplierAccountId">
          <el-select
            v-model="pullForm.supplierAccountId"
            placeholder="不选时拉取所选渠道下全部启用账户"
            filterable
            clearable
            :loading="pullAccountLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in pullAccountOptions"
              :key="item.id"
              :label="`${item.accountName}${item.pullChannel ? '（' + item.pullChannel + '）' : ''}`"
              :value="item.id"
            />
          </el-select>
          <div class="form-tip">
            一个账户对应一套密钥与一种上游协议。不选账户时会逐个拉取该渠道下所有启用账户，
            未配置拉取协议的账户会按通用地址尝试。
          </div>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="按流水号幂等：已存在的流水不会重复入库。客户/模型映射缺失的流水不会入库，结果里会给出未映射数量与样例。"
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitPull" :loading="pullSubmitting">开始拉取</el-button>
          <el-button @click="pullOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 上传流水 -->
    <el-dialog title="上传网关原始流水" v-model="uploadOpen" width="620px" append-to-body>
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadRef" label-width="110px">
        <el-form-item label="客户" prop="customerId">
          <el-select
            v-model="uploadForm.customerId"
            placeholder="请选择流水归属客户"
            filterable
            clearable
            :loading="customerLoading"
            style="width: 100%"
            @change="handleUploadCustomerChange"
          >
            <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <div class="form-tip">客户 → 渠道 → 账户 按该客户已签约合同的明细联动，一个文件归属一个客户</div>
        </el-form-item>
        <el-form-item label="渠道" prop="supplierId">
          <el-select
            v-model="uploadForm.supplierId"
            :placeholder="uploadForm.customerId ? '请选择流水所属渠道' : '请先选择客户'"
            filterable
            :disabled="!uploadForm.customerId"
            :loading="uploadSupplierLoading"
            style="width: 100%"
            @change="handleUploadSupplierChange"
          >
            <el-option
              v-for="item in uploadSupplierOptions"
              :key="item.supplierId"
              :label="item.supplierName"
              :value="item.supplierId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道账户" prop="supplierAccountId">
          <el-select
            v-model="uploadForm.supplierAccountId"
            :placeholder="uploadForm.supplierId ? '请选择渠道账户' : '请先选择渠道'"
            filterable
            :disabled="!uploadForm.supplierId"
            :loading="uploadAccountLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in uploadAccountOptions"
              :key="item.supplierAccountId"
              :label="`${item.supplierAccountName}${item.pullType === 2 ? '（文件上传）' : ''}${item.accountStatus === 0 ? '（已停用）' : ''}`"
              :value="item.supplierAccountId"
            />
          </el-select>
          <div class="form-tip">账户取自客户合同明细；合同未绑定账户时列出该渠道的启用账户</div>
        </el-form-item>
        <el-form-item label="流水文件">
          <el-upload
            ref="uploadFileRef"
            :limit="1"
            :auto-upload="false"
            accept=".xlsx,.xls"
            :on-change="handleUploadFileChange"
            :on-remove="handleUploadFileRemove"
            :on-exceed="handleUploadFileExceed"
            drag
            style="width: 100%"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">
                仅支持 xls / xlsx，单次最多 20000 行，文件不超过 10MB；
                <el-link type="primary" :underline="false" style="font-size: 12px" @click="handleUploadTemplate">下载模板</el-link>
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="入库后对账">
          <el-switch v-model="uploadForm.autoReconcile" />
          <span class="form-tip">按文件内调用时间区间立即对账，生成台账并匹配合同</span>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="客户 / 渠道 / 账户取自该客户已签约合同的明细，流水按此归属入库；按流水号幂等，模型解析不出来的行不入库。"
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitUpload" :loading="uploadSubmitting">开始上传</el-button>
          <el-button @click="uploadOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 台账详情 -->
    <el-dialog title="用量明细" v-model="usageOpen" width="820px" append-to-body>      <el-descriptions :column="2" border v-if="usageDetail">
        <el-descriptions-item label="流水号" :span="2">{{ usageDetail.callNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ usageDetail.customerName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ usageDetail.supplierName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="渠道账户">{{ usageDetail.supplierAccountName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="模型编码">{{ usageDetail.productCode }}</el-descriptions-item>
        <el-descriptions-item label="模型型号">{{ usageDetail.modelNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="匹配状态">
          <dict-tag :options="crm_usage_match_status" :value="String(usageDetail.matchStatus)" />
        </el-descriptions-item>
        <el-descriptions-item label="用量类型">
          <dict-tag :options="crm_usage_type" :value="String(usageDetail.usageType ?? 0)" />
        </el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ usageDetail.contactNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="账单编号">{{ usageDetail.billNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="token">{{ usageDetail.totalTokens }}</el-descriptions-item>
        <el-descriptions-item label="刊例价费用">{{ formatAmount(usageDetail.totalFees) }}</el-descriptions-item>
        <el-descriptions-item label="折后费用">{{ formatAmount(usageDetail.actualFees) }}</el-descriptions-item>
        <el-descriptions-item label="调用时间">{{ parseTime(usageDetail.callTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
        <el-descriptions-item label="基准售价(千token)">{{ formatAmount(usageDetail.baseSellPrice) }}</el-descriptions-item>
        <el-descriptions-item label="售价折扣">{{ usageDetail.sellDiscount ?? "-" }}</el-descriptions-item>
        <el-descriptions-item label="基准成本价(千token)">{{ formatAmount(usageDetail.baseCostPrice) }}</el-descriptions-item>
        <el-descriptions-item label="成本折扣">{{ usageDetail.costDiscount ?? "-" }}</el-descriptions-item>
        <el-descriptions-item label="应收金额">{{ formatAmount(usageDetail.sellAmount) }}</el-descriptions-item>
        <el-descriptions-item label="成本金额">{{ formatAmount(usageDetail.costAmount) }}</el-descriptions-item>
        <el-descriptions-item label="毛利">{{ formatAmount(usageDetail.grossProfit) }}</el-descriptions-item>
        <el-descriptions-item label="匹配说明" :span="2">{{ usageDetail.matchMsg || "-" }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="Usage">
import {
  listUsage,
  getUsage,
  reconcileUsage,
  rematchUsage,
  listCallLog,
  pullCallLog,
  uploadCallLog,
  getCustomerSupplierAccounts
} from "@/api/crm/usage"
import {
  submitGatewayLogExport,
  submitUsageExport,
  getExportTask,
  downloadExportFile
} from "@/api/crm/exportTask"
import { saveAs } from "file-saver"
import { getSupplierOptions } from "@/api/crm/supplier"
import { listSupplierAccount } from "@/api/crm/supplierAccount"
import { getCustomerOptions } from "@/api/crm/customer"
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()
const { crm_usage_match_status, crm_usage_type } = proxy.useDict("crm_usage_match_status", "crm_usage_type")

const activeTab = ref("usage")
const showSearch = ref(true)
const loading = ref(false)
const logLoading = ref(false)
const usageList = ref([])
const callLogList = ref([])
const total = ref(0)
const logTotal = ref(0)
const dateRange = ref([])
/** 网关原始流水默认只看前一天（昨日 00:00:00 ~ 23:59:59），与后端默认区间保持一致 */
function defaultLogDateRange() {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  const day = parseTime(yesterday, "{y}-{m}-{d}")
  return [day, day]
}
const logDateRange = ref(defaultLogDateRange())
const supplierOptions = ref([])
const supplierLoading = ref(false)
const customerOptions = ref([])
const customerLoading = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  callNo: undefined,
  customerId: undefined,
  supplierId: undefined,
  productCode: undefined,
  matchStatusList: []
})

const logQuery = ref({
  pageNum: 1,
  pageSize: 10,
  callNo: undefined,
  customerId: undefined,
  supplierId: undefined,
  productCode: undefined,
  callStatus: undefined
})

/** 匹配状态常量，与后端 UsageMatchStatusEnum 一致 */
const MATCH_SUCCESS = 1

/** 财务对账金额统一展示 8 位小数，与 decimal(20,8) 存储精度一致 */
function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toFixed(8)
}

function canRematch(row) {
  if (row.billId) {
    return false
  }
  if (Number(row.usageType || 0) === 1) {
    return false
  }
  return Number(row.matchStatus) !== MATCH_SUCCESS
}

function buildQuery() {
  const { matchStatusList, ...rest } = queryParams.value
  const query = { ...rest, params: {} }
  if (Array.isArray(matchStatusList) && matchStatusList.length) {
    query.params.matchStatusList = matchStatusList.join(",")
  }
  return proxy.addDateRange(query, dateRange.value)
}

function getList() {
  loading.value = true
  listUsage(buildQuery()).then(res => {
    usageList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function getCallLogList() {
  logLoading.value = true
  // 日期被清空时回到「前一天」，保证列表永远是受限区间而不是全量
  const range = logDateRange.value && logDateRange.value.length === 2 ? logDateRange.value : defaultLogDateRange()
  logDateRange.value = range
  const query = proxy.addDateRange({ ...logQuery.value, params: {} }, range)
  listCallLog(query).then(res => {
    callLogList.value = res.rows || []
    logTotal.value = res.total || 0
  }).finally(() => {
    logLoading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    callNo: undefined,
    customerId: undefined,
    supplierId: undefined,
    productCode: undefined,
    matchStatusList: []
  }
  getList()
}

function handleLogQuery() {
  logQuery.value.pageNum = 1
  getCallLogList()
}

function resetLogQuery() {
  logDateRange.value = defaultLogDateRange()
  logQuery.value = {
    pageNum: 1,
    pageSize: 10,
    callNo: undefined,
    customerId: undefined,
    supplierId: undefined,
    productCode: undefined,
    callStatus: undefined
  }
  getCallLogList()
}

/** 切换页签时懒加载对应数据 */
function handleTabChange(name) {
  if (name === "callLog" && callLogList.value.length === 0) {
    getCallLogList()
  }
}

/** 手动对账 */
const reconcileOpen = ref(false)
const reconcileSubmitting = ref(false)
const reconcileRef = ref()
const reconcileForm = ref({
  timeRange: [],
  supplierId: undefined,
  retryAbnormal: true
})

const reconcileRules = {
  timeRange: [{ required: true, message: "请选择调用时间区间", trigger: "change" }]
}

function openReconcile() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 1)
  reconcileForm.value = {
    timeRange: [parseTime(start, "{y}-{m}-{d} 00:00:00"), parseTime(end, "{y}-{m}-{d} 23:59:59")],
    supplierId: undefined,
    retryAbnormal: true
  }
  reconcileOpen.value = true
  nextTick(() => reconcileRef.value && reconcileRef.value.clearValidate())
}

function submitReconcile() {
  reconcileRef.value.validate(valid => {
    if (!valid) {
      return
    }
    reconcileSubmitting.value = true
    reconcileUsage({
      startTime: reconcileForm.value.timeRange[0],
      endTime: reconcileForm.value.timeRange[1],
      supplierId: reconcileForm.value.supplierId,
      retryAbnormal: reconcileForm.value.retryAbnormal
    }).then(res => {
      const r = res.data || {}
      proxy.$modal.msgSuccess(
        `对账完成：扫描 ${r.scanned || 0} 条，新增台账 ${r.created || 0} 条，成功 ${r.matched || 0} 条，异常 ${r.abnormal || 0} 条，新增差异 ${r.diffCreated || 0} 条`
      )
      reconcileOpen.value = false
      getList()
    }).finally(() => {
      reconcileSubmitting.value = false
    })
  })
}

/** 拉取流水：按渠道 + 渠道账户从上游拉取 */
const pullOpen = ref(false)
const pullSubmitting = ref(false)
const pullRef = ref()
const pullAccountLoading = ref(false)
const pullAccountOptions = ref([])
const pullForm = ref({
  timeRange: [],
  supplierId: undefined,
  supplierAccountId: undefined
})

const pullRules = {
  timeRange: [{ required: true, message: "请选择调用时间区间", trigger: "change" }]
}

function openPull() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 1)
  pullForm.value = {
    timeRange: [parseTime(start, "{y}-{m}-{d} 00:00:00"), parseTime(end, "{y}-{m}-{d} 23:59:59")],
    supplierId: undefined,
    supplierAccountId: undefined
  }
  pullAccountOptions.value = []
  loadPullAccounts()
  pullOpen.value = true
  nextTick(() => pullRef.value && pullRef.value.clearValidate())
}

/** 账户选项：只取启用账户，选渠道后收窄范围 */
function loadPullAccounts() {
  pullAccountLoading.value = true
  listSupplierAccount({ pageNum: 1, pageSize: 500, status: 1, supplierId: pullForm.value.supplierId })
    .then(res => {
      pullAccountOptions.value = res.rows || []
    })
    .finally(() => {
      pullAccountLoading.value = false
    })
}

function handlePullSupplierChange() {
  // 渠道变了，已选账户可能不属于新渠道，必须清掉
  pullForm.value.supplierAccountId = undefined
  loadPullAccounts()
}

function submitPull() {
  pullRef.value.validate(valid => {
    if (!valid) {
      return
    }
    pullSubmitting.value = true
    pullCallLog({
      startTime: pullForm.value.timeRange[0],
      endTime: pullForm.value.timeRange[1],
      supplierId: pullForm.value.supplierId,
      supplierAccountId: pullForm.value.supplierAccountId
    }).then(res => {
      const r = res.data || {}
      let msg = `拉取完成：共 ${r.pulled || 0} 条，入库 ${r.inserted || 0} 条，重复 ${r.duplicated || 0} 条`
      if (r.unmapped) {
        msg += `；未映射客户 ${r.unmapped} 条`
      }
      if (r.missingModel) {
        msg += `；缺模型编码 ${r.missingModel} 条`
      }
      proxy.$modal.msgSuccess(msg)
      // 有未映射时给出样例，便于直接去「流水映射配置」补规则
      if (r.unmapped > 0 && r.unmappedSample && r.unmappedSample.length) {
        proxy.$alert(
          `以下上游标识未配置客户映射，对应流水未入库：${r.unmappedSample.join("、")}`,
          "存在未映射的上游客户",
          { type: "warning" }
        )
      }
      pullOpen.value = false
      getCallLogList()
    }).catch(e => {
      // 单账户失败时后端会把原因放进 accounts[].error，这里直接展示首个错误
      const accounts = (e && e.data && e.data.accounts) || []
      const first = accounts.find(item => item.error)
      proxy.$modal.msgError((e && e.message) || (first && first.error) || "拉取失败")
    }).finally(() => {
      pullSubmitting.value = false
    })
  })
}

/** 上传流水：人工上传网关导出的流水文件，按渠道 + 账户解析客户与模型后入库 */
const uploadOpen = ref(false)
const uploadSubmitting = ref(false)
const uploadRef = ref()
const uploadFileRef = ref()
const uploadFile = ref(null)
const uploadAccountLoading = ref(false)
const uploadSupplierLoading = ref(false)
const uploadAccountOptions = ref([])
const uploadSupplierOptions = ref([])
/** 客户合同里的「渠道 + 渠道账户」组合，作为渠道、账户两级下拉的数据源 */
const uploadCombos = ref([])
/** 客户快速切换时，只接受最后一次请求的响应，避免旧响应覆盖新客户的渠道 */
let uploadComboSeq = 0
/** 渠道快速切换时同理，兜底账户请求只认最后一次 */
let uploadAccountSeq = 0
const uploadForm = ref({
  customerId: undefined,
  supplierId: undefined,
  supplierAccountId: undefined,
  autoReconcile: true
})

const uploadRules = {
  customerId: [{ required: true, message: "请选择流水归属客户", trigger: "change" }],
  supplierId: [{ required: true, message: "请选择流水所属渠道", trigger: "change" }],
  supplierAccountId: [{ required: true, message: "请选择渠道账户", trigger: "change" }]
}

function openUpload() {
  // 重开弹窗时作废上一轮在途请求，避免旧响应回填到空表单
  uploadComboSeq++
  uploadAccountSeq++
  uploadForm.value = {
    customerId: undefined,
    supplierId: undefined,
    supplierAccountId: undefined,
    autoReconcile: true
  }
  uploadFile.value = null
  uploadCombos.value = []
  uploadSupplierOptions.value = []
  uploadAccountOptions.value = []
  uploadOpen.value = true
  nextTick(() => {
    uploadRef.value && uploadRef.value.clearValidate()
    uploadFileRef.value && uploadFileRef.value.clearFiles()
  })
}

/** 选客户：渠道与账户都来自该客户生效合同的明细行，因此客户一变必须重算并清空下游 */
function handleUploadCustomerChange() {
  // 作废在途请求：客户被清空或快速切换时，旧响应不能落到新客户的下拉上
  const seq = ++uploadComboSeq
  uploadForm.value.supplierId = undefined
  uploadForm.value.supplierAccountId = undefined
  uploadCombos.value = []
  uploadSupplierOptions.value = []
  uploadAccountOptions.value = []
  const customerId = uploadForm.value.customerId
  if (!customerId) {
    uploadSupplierLoading.value = false
    return
  }
  uploadSupplierLoading.value = true
  getCustomerSupplierAccounts(customerId).then(res => {
    if (seq !== uploadComboSeq) {
      return
    }
    const list = res.data || []
    uploadCombos.value = list
    const supplierMap = new Map()
    list.forEach(item => {
      if (item.supplierId && !supplierMap.has(item.supplierId)) {
        supplierMap.set(item.supplierId, {
          supplierId: item.supplierId,
          supplierName: item.supplierName || `渠道${item.supplierId}`
        })
      }
    })
    uploadSupplierOptions.value = Array.from(supplierMap.values())
    if (!list.length) {
      proxy.$modal.msgWarning("该客户暂无已签约合同，请先在合同明细中配置渠道与账户")
    }
  }).finally(() => {
    if (seq === uploadComboSeq) {
      uploadSupplierLoading.value = false
    }
  })
}

/** 选渠道：账户只取该客户在这个渠道合同明细上的账户（去重） */
function handleUploadSupplierChange() {
  const seq = ++uploadAccountSeq
  uploadForm.value.supplierAccountId = undefined
  uploadAccountOptions.value = []
  const supplierId = uploadForm.value.supplierId
  if (!supplierId) {
    uploadAccountLoading.value = false
    return
  }
  const seen = new Set()
  const options = []
  uploadCombos.value.filter(item => item.supplierId === supplierId).forEach(item => {
    if (!item.supplierAccountId || seen.has(item.supplierAccountId)) {
      return
    }
    seen.add(item.supplierAccountId)
    options.push({
      supplierAccountId: item.supplierAccountId,
      supplierAccountName: item.supplierAccountName || `账户${item.supplierAccountId}`,
      accountStatus: item.accountStatus,
      pullType: item.pullType
    })
  })
  if (options.length) {
    uploadAccountOptions.value = options
    return
  }
  // 历史合同明细未绑定账户时，回退到该渠道的启用账户，避免老合同无法上传
  loadUploadFallbackAccounts(supplierId, seq)
}

/** 合同明细没有账户时的兜底：取该渠道启用账户，字段与合同组合对齐，模板无需分支 */
function loadUploadFallbackAccounts(supplierId, seq) {
  uploadAccountLoading.value = true
  listSupplierAccount({ pageNum: 1, pageSize: 500, status: 1, supplierId })
    .then(res => {
      if (seq !== uploadAccountSeq) {
        return
      }
      uploadAccountOptions.value = (res.rows || []).map(item => ({
        supplierAccountId: item.id,
        supplierAccountName: item.accountName,
        accountStatus: item.status,
        pullType: item.pullType
      }))
    })
    .finally(() => {
      if (seq === uploadAccountSeq) {
        uploadAccountLoading.value = false
      }
    })
}

function handleUploadFileChange(file) {
  uploadFile.value = file
}

function handleUploadFileRemove() {
  uploadFile.value = null
}

/** 只允许一个文件：重新选择时覆盖上一个，避免误传多个文件 */
function handleUploadFileExceed(files) {
  const el = uploadFileRef.value
  if (!el) {
    return
  }
  el.clearFiles()
  const file = files[0]
  file.uid = Date.now()
  el.handleStart(file)
}

function handleUploadTemplate() {
  proxy.download("crm/gateway-call-log/upload-template", {}, `网关原始流水导入模板_${new Date().getTime()}.xlsx`)
}

function submitUpload() {
  uploadRef.value.validate(valid => {
    if (!valid) {
      return
    }
    const raw = uploadFile.value && uploadFile.value.raw
    if (!raw) {
      proxy.$modal.msgError("请选择要上传的流水文件")
      return
    }
    const formData = new FormData()
    formData.append("file", raw)
    formData.append("customerId", uploadForm.value.customerId)
    formData.append("supplierId", uploadForm.value.supplierId)
    formData.append("supplierAccountId", uploadForm.value.supplierAccountId)
    formData.append("autoReconcile", uploadForm.value.autoReconcile ? "true" : "false")
    uploadSubmitting.value = true
    uploadCallLog(formData).then(res => {
      const r = res.data || {}
      let msg = res.msg || "上传完成"
      if (r.reconcile) {
        msg += `；已对账：新增台账 ${r.reconcile.created || 0} 条，匹配成功 ${r.reconcile.matched || 0} 条，异常 ${r.reconcile.abnormal || 0} 条`
      }
      proxy.$modal.msgSuccess(msg)
      // 未入库的行按原因给出样例，便于直接去「流水映射配置」补规则或修文件
      const warnings = []
      if (r.missingModel > 0 && r.missingModelSample && r.missingModelSample.length) {
        warnings.push(`缺模型编码 ${r.missingModel} 条，样例：${r.missingModelSample.join("、")}`)
      }
      if (r.invalid > 0 && r.rowErrors && r.rowErrors.length) {
        warnings.push(`无效行 ${r.invalid} 条，前 ${r.rowErrors.length} 条：${r.rowErrors.join("；")}`)
      }
      if (warnings.length) {
        proxy.$alert(warnings.join("<br/>"), "部分数据未入库", { type: "warning", dangerouslyUseHTMLString: true })
      }
      uploadOpen.value = false
      getCallLogList()
      getList()
    }).catch(() => {
      // 失败提示由请求拦截器统一弹出后端 msg，这里不重复提示
    }).finally(() => {
      uploadSubmitting.value = false
    })
  })
}

/** 单条重新匹配 */
function handleRematch(row) {
  proxy.$modal.confirm(`确认重新匹配流水【${row.callNo}】？`).then(() => {
    return rematchUsage(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("匹配成功，已按当前生效合同计费")
    getList()
  }).catch(() => {})
}

/** 台账详情 */
const usageOpen = ref(false)
const usageDetail = ref(null)

function handleViewUsage(row) {
  getUsage(row.id).then(res => {
    usageDetail.value = res.data
    usageOpen.value = true
  })
}

/** 异步导出：提交任务 → 轮询 → 自动下载；台账与流水共用一套流程 */
const exportLoading = ref({ usage: false, callLog: false })
/** 在途轮询定时器，页面卸载时统一清理，避免离开页面后仍在打接口 */
const exportPollTimers = new Set()
const EXPORT_POLL_INTERVAL = 2000
const EXPORT_POLL_TIMEOUT = 10 * 60 * 1000

function handleExport() {
  submitAsyncExport("usage", submitUsageExport, buildQuery())
}

function handleLogExport() {
  // 与列表同口径：导出同样限定调用时间区间，缺失时按前一天
  const range = logDateRange.value && logDateRange.value.length === 2 ? logDateRange.value : defaultLogDateRange()
  const query = proxy.addDateRange({ ...logQuery.value, params: {} }, range)
  submitAsyncExport("callLog", submitGatewayLogExport, query)
}

/** 业务侧错误：请求拦截器不会提示，由导出流程统一弹出 */
function exportError(message) {
  const error = new Error(message)
  error.exportHandled = true
  return error
}

/**
 * 提交异步导出任务并等待结果
 *
 * @param {string} key 按钮状态键（usage / callLog）
 * @param {Function} submitter 提交接口
 * @param {object} query 查询条件
 */
async function submitAsyncExport(key, submitter, query) {
  if (exportLoading.value[key]) {
    return
  }
  exportLoading.value[key] = true
  try {
    const res = await submitter(query)
    const taskId = res.data
    if (!taskId) {
      throw exportError("导出任务提交失败，请稍后重试")
    }
    proxy.$modal.msgSuccess(res.msg || "导出任务已提交，生成完成后会自动下载")
    const task = await pollExportTask(taskId)
    await downloadExportTask(task)
    proxy.$modal.msgSuccess(`导出完成${task.rowCount ? `，共 ${task.rowCount} 条` : ""}`)
  } catch (e) {
    // 网络/超时/业务码错误拦截器已提示过，这里只补业务侧自定义错误，避免重复弹窗
    if (e && e.exportHandled) {
      proxy.$modal.msgError(e.message)
    }
  } finally {
    exportLoading.value[key] = false
  }
}

/**
 * 轮询任务状态：0待处理 / 1处理中继续等，2成功返回任务，3失败或超时抛错
 *
 * @param {number} taskId 任务ID
 * @returns {Promise<object>} 成功后的任务
 */
function pollExportTask(taskId) {
  return new Promise((resolve, reject) => {
    const startedAt = Date.now()
    const timer = setInterval(() => {
      getExportTask(taskId).then(res => {
        const task = res.data || {}
        if (task.status === 2) {
          finishPoll(timer)
          resolve(task)
        } else if (task.status === 3) {
          finishPoll(timer)
          reject(exportError(task.errMsg || "导出失败，请稍后重试"))
        } else if (Date.now() - startedAt > EXPORT_POLL_TIMEOUT) {
          finishPoll(timer)
          reject(exportError("导出耗时较长，请稍后重新发起导出"))
        }
      }).catch(e => {
        finishPoll(timer)
        reject(e)
      })
    }, EXPORT_POLL_INTERVAL)
    exportPollTimers.add(timer)
  })
}

function finishPoll(timer) {
  clearInterval(timer)
  exportPollTimers.delete(timer)
}

/** 下载结果文件；后端异常时 blob 里其实是 JSON 错误体，需要还原成可读提示 */
async function downloadExportTask(task) {
  const data = await downloadExportFile(task.id)
  if (!(data instanceof Blob) || !data.type || data.type.indexOf("json") > -1) {
    let message = "导出文件下载失败，请稍后重试"
    try {
      const body = JSON.parse(await data.text())
      message = body.msg || message
    } catch (ignore) {
      // 保留默认提示
    }
    throw exportError(message)
  }
  saveAs(new Blob([data], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }), task.fileName || `导出数据_${Date.now()}.xlsx`)
}

onBeforeUnmount(() => {
  exportPollTimers.forEach(timer => clearInterval(timer))
  exportPollTimers.clear()
})

function loadSupplierOptions() {
  supplierLoading.value = true
  getSupplierOptions().then(res => {
    supplierOptions.value = res.data || []
  }).finally(() => {
    supplierLoading.value = false
  })
}

function loadCustomerOptions() {
  customerLoading.value = true
  getCustomerOptions().then(res => {
    customerOptions.value = res.data || []
  }).finally(() => {
    customerLoading.value = false
  })
}

loadSupplierOptions()
loadCustomerOptions()
getList()
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
