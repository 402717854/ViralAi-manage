<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="账户名" prop="accountName">
        <el-input
          v-model="queryParams.accountName"
          placeholder="请输入账户名"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="apiKey编号" prop="apiKeyNo">
        <el-input
          v-model="queryParams.apiKeyNo"
          placeholder="请输入 apiKey 编号"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierId">
        <el-select
          v-model="queryParams.supplierId"
          placeholder="请选择供应商"
          clearable
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="账户类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['crm:account:add']"
        >新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="accountList">
      <el-table-column label="账户名" align="center" prop="accountName" min-width="140" show-overflow-tooltip />
      <el-table-column label="供应商" align="center" prop="supplierName" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.supplierName || `编号 ${scope.row.supplierId}` }}</template>
      </el-table-column>
      <el-table-column label="apiKey编号" align="center" prop="apiKeyNo" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.apiKeyNo || "-" }}</template>
      </el-table-column>
      <el-table-column label="apiKey" align="center" prop="apiKey" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <span class="mono">{{ scope.row.apiKey || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="账户类型" align="center" prop="type" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.type === 2 ? 'success' : 'info'" effect="plain">
            {{ typeLabel(scope.row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="拉取协议" align="center" prop="pullChannel" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.pullChannel" type="warning" effect="plain">
            {{ scope.row.pullChannel }}
          </el-tag>
          <span v-else>通用</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" effect="plain">
            {{ statusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建者" align="center" prop="creatorName" width="120">
        <template #default="scope">{{ scope.row.creatorName || "-" }}</template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="165" />
      <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['crm:account:edit']"
          >修改</el-button>
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

    <!-- 添加或修改渠道账户对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="accountRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="供应商" prop="supplierId">
          <el-select
            v-model="form.supplierId"
            placeholder="请选择供应商"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in supplierSelectOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账户名" prop="accountName">
          <el-input v-model="form.accountName" placeholder="请输入账户名" maxlength="100" />
        </el-form-item>
        <el-form-item label="账户密码" prop="accountPwd">
          <el-input
            v-model="form.accountPwd"
            type="password"
            show-password
            maxlength="255"
            :placeholder="form.id ? '不填写则保持原密码不变' : '请输入账户密码'"
          />
        </el-form-item>
        <el-form-item label="apiKey编号" prop="apiKeyNo">
          <el-input v-model="form.apiKeyNo" placeholder="请输入 apiKey 编号" maxlength="100" />
        </el-form-item>
        <el-form-item label="apiKey" prop="apiKey">
          <el-input
            v-model="form.apiKey"
            type="textarea"
            :rows="2"
            placeholder="请输入 apiKey"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="账户类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio v-for="item in typeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-divider content-position="left">流水拉取</el-divider>
        <el-form-item label="拉取协议" prop="pullChannel">
          <el-select v-model="form.pullChannel" placeholder="不配置则走通用拉取地址" clearable style="width: 100%">
            <el-option v-for="item in pullChannelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <div class="form-tip">
            账户拉取协议决定接口形态：viralai 中转站为 GET + Bearer 令牌 + 页码分页；
            pixverse 为 POST + API-KEY 头 + 游标分页，且单次区间不超过 30 天。
          </div>
        </el-form-item>
        <el-form-item label="拉取地址" prop="pullUrl">
          <el-input
            v-model="form.pullUrl"
            placeholder="留空使用该协议的默认地址"
            maxlength="255"
            clearable
          />
          <div class="form-tip">
            pixverse 有 .ai（国际）与 .cn（国内）两套域名，API Key 必须与域名同地域；
            自建中转站也在这里覆盖。
          </div>
        </el-form-item>
        <el-form-item label="拉取用户名" prop="pullUser">
          <el-input
            v-model="form.pullUser"
            placeholder="留空则拉取全量（不按上游用户名过滤）"
            maxlength="100"
            clearable
          />
          <div class="form-tip">仅 viralai 中转站的 /api/log/ 支持按 username 过滤。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SupplierAccount">
import {
  listSupplierAccount,
  getSupplierAccount,
  addSupplierAccount,
  updateSupplierAccount
} from "@/api/crm/supplierAccount"
import { getSupplierOptions } from "@/api/crm/supplier"

const { proxy } = getCurrentInstance()

const accountList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref("")
const submitting = ref(false)

/** 供应商下拉选项：来源 /crm/supplier/options，仅返回启用状态的渠道 */
const supplierOptions = ref([])

/** 账户类型：与后端 type 约定一致（1测试账户 2真实账户） */
const typeOptions = [
  { label: "测试账户", value: 1 },
  { label: "真实账户", value: 2 }
]

/** 状态：0未启用 1启用 */
const statusOptions = [
  { label: "启用", value: 1 },
  { label: "未启用", value: 0 }
]

/**
 * 流水拉取协议
 * <p>与后端 GatewayPullAdapter 的 channel() 一一对应；不选则走通用拉取地址</p>
 */
const pullChannelOptions = [
  { label: "viralai 中转站", value: "viralaiai" },
  { label: "pixverse", value: "pixverse" }
]

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    accountName: undefined,
    apiKeyNo: undefined,
    supplierId: undefined,
    type: undefined,
    status: undefined
  },
  rules: {
    supplierId: [{ required: true, message: "请选择供应商", trigger: "change" }],
    accountName: [{ required: true, message: "账户名不能为空", trigger: "blur" }],
    apiKey: [{ required: true, message: "apiKey 不能为空", trigger: "blur" }],
    type: [{ required: true, message: "请选择账户类型", trigger: "change" }],
    status: [{ required: true, message: "请选择状态", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/**
 * 编辑表单的供应商选项
 * <p>下拉只加载启用状态的渠道；若账户关联的渠道已停用或删除，
 * 补齐一条只读占位项，避免回显成裸露的 ID</p>
 */
const supplierSelectOptions = computed(() => {
  const list = supplierOptions.value
  const current = form.value.supplierId
  if (current === null || current === undefined || current === "") {
    return list
  }
  if (list.some(item => String(item.id) === String(current))) {
    return list
  }
  return [{ id: current, name: `编号 ${current}（渠道已停用或不存在）` }, ...list]
})

/** 类型文案 */
function typeLabel(value) {
  const hit = typeOptions.find(item => item.value === value)
  return hit ? hit.label : "-"
}

/** 状态文案 */
function statusLabel(value) {
  const hit = statusOptions.find(item => item.value === value)
  return hit ? hit.label : "-"
}

/** 加载供应商下拉选项 */
function loadSupplierOptions() {
  getSupplierOptions().then(response => {
    const list = Array.isArray(response.data) ? response.data : (response.rows || [])
    supplierOptions.value = list.map(item => ({ id: item.id, name: item.name }))
  }).catch(() => {})
}

/** 查询渠道账户列表 */
function getList() {
  loading.value = true
  listSupplierAccount(queryParams.value).then(response => {
    accountList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => {
    loading.value = false
  })
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 取消 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    supplierId: undefined,
    accountName: undefined,
    accountPwd: undefined,
    apiKeyNo: undefined,
    apiKey: undefined,
    type: 2,
    status: 1,
    pullChannel: undefined,
    pullUrl: undefined,
    pullUser: undefined
  }
  proxy.resetForm("accountRef")
}

/** 新增 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增渠道账户"
}

/** 修改 */
function handleUpdate(row) {
  reset()
  const id = row.id
  getSupplierAccount(id).then(response => {
    const data = response.data || {}
    form.value = {
      ...data,
      // 密码不返回明文，留空即保持不变
      accountPwd: ""
    }
    open.value = true
    title.value = "修改渠道账户"
  })
}

/** 提交 */
function submitForm() {
  proxy.$refs["accountRef"].validate(valid => {
    if (!valid) {
      return
    }
    submitting.value = true
    // 只提交可写字段，避免把详情里的 creatorName / createTime 等只读字段回传
    const payload = {
      id: form.value.id,
      supplierId: form.value.supplierId,
      accountName: form.value.accountName,
      accountPwd: form.value.accountPwd,
      apiKeyNo: form.value.apiKeyNo,
      apiKey: form.value.apiKey,
      type: form.value.type,
      status: form.value.status
    }
    const submit = payload.id ? updateSupplierAccount : addSupplierAccount
    submit(payload).then(() => {
      proxy.$modal.msgSuccess(form.value.id ? "修改成功" : "新增成功")
      open.value = false
      getList()
    }).finally(() => {
      submitting.value = false
    })
  })
}

loadSupplierOptions()
getList()
</script>

<style scoped>
.mono {
  font-family: Consolas, Monaco, "Courier New", monospace;
  font-size: 12px;
}
.form-tip {
  width: 100%;
  line-height: 1.5;
  padding-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
