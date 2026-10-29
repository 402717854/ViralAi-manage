<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模型厂商" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入模型厂商"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模型型号" prop="no">
        <el-input
          v-model="queryParams.no"
          placeholder="请输入模型型号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模型类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部" clearable style="width: 160px">
          <el-option
            v-for="item in modelTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
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
          v-hasPermi="['crm:product:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['crm:product:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['crm:product:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['crm:product:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模型厂商" align="center" prop="name" show-overflow-tooltip />
      <el-table-column label="模型型号" align="center" prop="no" show-overflow-tooltip />
      <el-table-column label="模型类型" align="center" prop="type" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.type != null" size="small" type="info">{{ modelTypeLabel(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="价格明细" align="center" prop="priceDetail" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success" size="small">有效</el-tag>
          <el-tag v-else type="info" size="small">失效</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建者" align="center" prop="creator" width="110" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:product:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:product:remove']">删除</el-button>
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

    <!-- 添加或修改CRM模型产品对话框 -->
    <el-dialog :title="title" v-model="open" width="1200px" append-to-body>
      <el-form ref="productRef" :model="form" :rules="rules" label-width="110px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="模型厂商" prop="name">
              <el-input v-model="form.name" placeholder="请输入模型厂商" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模型型号" prop="no">
              <el-input v-model="form.no" placeholder="请输入模型型号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模型类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择模型类型" style="width: 100%">
                <el-option
                  v-for="item in modelTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="价格明细" prop="priceDetail">
              <el-input v-model="form.priceDetail" type="textarea" :rows="2" placeholder="请输入价格明细" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="产品描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入产品描述" />
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

<script setup name="Product">
import { listProduct, getProduct, delProduct, addProduct, updateProduct } from "@/api/crm/product"

const { proxy } = getCurrentInstance()

const productList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const { crm_deleted,crm_bus_status,crm_bus_contact,crm_bus_settle } = useDict('crm_deleted','crm_bus_status','crm_bus_contact','crm_bus_settle')

/** 模型类型选项：取值与后端 CrmModelProduct#type 注释一致（1文本 2多模态 3图片处理 4音频处理 5视频处理） */
const modelTypeOptions = [
  { label: "文本", value: 1 },
  { label: "多模态", value: 2 },
  { label: "图片处理", value: 3 },
  { label: "音频处理", value: 4 },
  { label: "视频处理", value: 5 }
]

/** 状态选项：与后端 status（Integer）保持数值类型，避免下拉回显因字符串/数字不匹配而失效 */
const statusOptions = [
  { label: "有效", value: 1 },
  { label: "失效", value: 0 }
]

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    no: undefined,
    type: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: "模型厂商不能为空", trigger: "blur" }],
    no: [{ required: true, message: "模型型号不能为空", trigger: "blur" }],
    type: [{ required: true, message: "模型类型不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 模型类型标签 */
function modelTypeLabel(value) {
  const hit = modelTypeOptions.find(item => item.value === value)
  return hit ? hit.label : value
}


/** 查询CRM模型产品列表 */
function getList() {
  loading.value = true
  listProduct(queryParams.value).then(response => {
    productList.value = response.rows
    total.value = response.total
  }).finally(() => {
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
    no: null,
    type: null,
    source: null,
    priceDetail: null,
    status: 1,
    description: null
  }
  proxy.resetForm("productRef")
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
  title.value = "添加CRM模型产品"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value[0]
  getProduct(_id).then(response => {
    const data = response.data || {}
    form.value = { ...data }
    open.value = true
    title.value = "修改CRM模型产品"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["productRef"].validate(valid => {
    if (!valid) {
      return
    }
    const payload = { ...form.value }
    if (payload.id != null) {
      updateProduct(payload).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addProduct(payload).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除CRM模型产品编号为"' + _ids + '"的数据项？').then(function () {
    return delProduct(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('crm/product/export', {
    ...queryParams.value
  }, `product_${new Date().getTime()}.xlsx`)
}

getList()
</script>
