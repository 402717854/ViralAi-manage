<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="客户名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入客户名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="负责人" prop="ownerUser">
        <el-input
          v-model="queryParams.ownerUser"
          placeholder="请选择负责人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机" prop="mobile">
        <el-input
          v-model="queryParams.mobile"
          placeholder="请输入手机"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="queryParams.email"
          placeholder="请输入邮箱"
          clearable
          @keyup.enter="handleQuery"
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
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['crm:customer:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['crm:customer:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['crm:customer:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['crm:customer:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="customerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" fixed="left" />
      <el-table-column label="客户名称" align="center" prop="name" width="180" fixed="left" :show-overflow-tooltip="true" />
      <el-table-column label="负责人" align="center" prop="ownerUser" width="100" fixed="left" />
      <el-table-column label="客户状态" align="center" prop="status" width="100" fixed="left">
        <template #default="scope">
          <dict-tag :options="crm_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="手机" align="center" prop="mobile" width="180"/>
      <el-table-column label="邮箱" align="center" prop="email" width="180"/>
      <el-table-column label="地区" align="center" prop="detailAddress" />
      <el-table-column label="所属行业" align="center" prop="industry" />
      <el-table-column label="创建者" align="center" prop="creator" />
      <el-table-column label="创建日期" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新者" align="center" prop="updater" />
      <el-table-column label="更新日期" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否删除" align="center" prop="deleted">
        <template #default="scope">
          <dict-tag :options="crm_deleted" :value="scope.row.deleted" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleViewData(scope.row)" v-hasPermi="['crm:customer:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:customer:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:customer:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- CRM 客户详情抽屉 -->
    <customer-view-drawer ref="customerViewRef" />
    <!-- 添加或修改CRM 客户对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="customerRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="客户名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="负责人" prop="ownerUser">
              <el-input v-model="form.ownerUser" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="手机" prop="mobile">
              <el-input v-model="form.mobile" placeholder="请输入手机" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属地区" prop="detailAddress">
              <el-input v-model="form.detailAddress" placeholder="请输入客户所属地区" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属行业" prop="industry">
              <el-input v-model="form.industry" placeholder="请输入客户所属行业" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="客户状态" prop="status">
              <el-select v-model="form.status">
                <el-option
                    v-for="item in crm_status"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Customer">
import { listCustomer, getCustomer, delCustomer, addCustomer, updateCustomer } from "@/api/crm/customer"
import CustomerViewDrawer from "./view"

const { proxy } = getCurrentInstance()

const { crm_customer_level,crm_status,crm_deleted } = useDict('crm_deleted','crm_customer_level','crm_status')

// 字典字段：字典项的 value 取自 sys_dict_data.dict_value（varchar，JSON 里是字符串），
// 而后端这些字段是 Long/Integer（JSON 里是数字）。el-select 按全等（===）匹配 option，
// 类型不一致就匹配不到 label，会把原始值直接当文本显示。回显前统一转成字符串。
const DICT_FIELDS = ['level', 'source', 'status', 'deleted']
const customerList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    ownerUser: undefined,
    mobile: undefined,
    email: undefined,
  },
  rules: {
    name: [
      { required: true, message: "客户名称不能为空", trigger: "blur" }
    ],
    ownerUser: [
      { required: true, message: "负责人不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "客户状态不能为空", trigger: "change" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询CRM 客户列表 */
function getList() {
  loading.value = true
  listCustomer(queryParams.value).then(response => {
    customerList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    name: null,
    ownerUser: null,
    status: null,
    mobile: null,
    email: null,
    detailAddress: null,
    industry: null,
    remark: null
  }
  proxy.resetForm("customerRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加CRM 客户"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getCustomer(_id).then(response => {
    const detail = response.data || {}
    DICT_FIELDS.forEach(key => {
      if (detail[key] !== null && detail[key] !== undefined && detail[key] !== '') {
        detail[key] = String(detail[key])
      }
    })
    form.value = detail
    open.value = true
    title.value = "修改CRM 客户"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["customerRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateCustomer(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCustomer(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除CRM 客户编号为"' + _ids + '"的数据项？').then(function() {
    return delCustomer(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 详情按钮操作 */
function handleViewData(row) {
  proxy.$refs["customerViewRef"].open(row.id)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('crm/customer/export', {
    ...queryParams.value
  }, `customer_${new Date().getTime()}.xlsx`)
}

getList()
</script>
