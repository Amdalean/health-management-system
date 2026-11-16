<template>
  <view class="form-container" :class="themeClass">
    <!-- 快捷初始化提示 -->
    <view class="init-tip" v-if="isAdd && form.startDeposit > 0">
      <uni-icons type="info" size="14" color="#409EFF"></uni-icons>
      <text class="init-tip-text">已自动带入上月结余 ¥{{ form.startDeposit }} 作为本月初</text>
      <text class="init-tip-btn" @click="reloadInit">重填</text>
    </view>

    <!-- 表单 -->
    <view class="form-section">
      <view class="form-group">
        <text class="form-label">年度月份<text class="required">*</text></text>
        <picker mode="date" fields="month" :value="yearMonth" @change="onYearMonthChange">
          <view class="picker-input" :class="{ placeholder: !yearMonth }">
            <uni-icons type="calendar" size="16" color="#909399" style="margin-right:6px;"></uni-icons>
            <text>{{ yearMonth || '请选择年月' }}</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">备注</text>
        <uni-easyinput v-model="form.remark" placeholder="例如：8月汇总" :inputBorder="false" />
      </view>

      <view class="form-group">
        <text class="form-label">收入（元）<text class="required">*</text></text>
        <view class="amount-input-wrap">
          <text class="currency-sign">¥</text>
          <input class="amount-input" v-model="form.income" type="digit" inputmode="decimal" placeholder="0.00" :adjust-position="true" @input="onIncomeInput" />
        </view>
        <view class="quick-chips">
          <view v-for="v in quickAmounts" :key="v" class="chip" @click="form.income=String(v);onIncomeInput()"><text class="chip-text">¥{{ v }}</text></view>
        </view>
      </view>

      <view class="auto-grid">
        <view class="auto-item"><text class="auto-label">支出</text><text class="auto-val">¥{{ form.expense }}</text></view>
        <view class="auto-item"><text class="auto-label">结余</text><text class="auto-val">¥{{ form.balance }}</text></view>
        <view class="auto-item"><text class="auto-label">月初存款</text><text class="auto-val">¥{{ form.startDeposit }}</text></view>
        <view class="auto-item"><text class="auto-label">月底存款</text><text class="auto-val auto-val--bold">¥{{ form.endDeposit }}</text></view>
      </view>

      <!-- 明细 -->
      <view class="detail-header">
        <text class="detail-title">财务明细</text>
        <view class="detail-actions">
          <view class="detail-btn detail-btn--add" @click="handleAddDetail"><uni-icons type="plusempty" size="14" color="#fff"></uni-icons><text class="detail-btn-text">添加</text></view>
          <view class="detail-btn detail-btn--del" @click="handleDelDetail"><uni-icons type="trash" size="14" color="#F56C6C"></uni-icons><text class="detail-btn-text" style="color:#F56C6C">删除</text></view>
        </view>
      </view>

      <view v-for="(row, idx) in detailList" :key="idx" class="detail-card" :class="{ 'detail-card--checked': checkedIdx.includes(idx) }" @click="toggleCheck(idx)">
        <view class="detail-top">
          <view class="check-box" :class="{ 'check-box--on': checkedIdx.includes(idx) }"><uni-icons v-if="checkedIdx.includes(idx)" type="checkmarkempty" size="12" color="#fff"></uni-icons></view>
          <text class="detail-index">#{{ idx+1 }}</text>
          <input class="detail-name" v-model="row.name" placeholder="名称（选填）" @click.stop />
        </view>
        <view class="detail-row">
          <view class="detail-col">
            <text class="detail-label">类别</text>
            <picker :value="catIndex(row.category)" :range="ckOptions" range-key="label" @change="e=>row.category=ckOptions[e.detail.value].value" @click.stop>
              <view class="detail-picker"><text class="picker-text">{{ catLabel(row.category) }}</text><uni-icons type="arrowdown" size="12" color="#909399"></uni-icons></view>
            </picker>
          </view>
          <view class="detail-col">
            <text class="detail-label">类型</text>
            <picker :value="typeIndex(row.type)" :range="szOptions" range-key="label" @change="e=>row.type=szOptions[e.detail.value].value" @click.stop>
              <view class="detail-picker"><text class="picker-text">{{ typeLabel(row.type) }}</text><uni-icons type="arrowdown" size="12" color="#909399"></uni-icons></view>
            </picker>
          </view>
        </view>
        <view class="detail-row">
          <view class="detail-col" style="flex:1">
            <text class="detail-label">金额（支持 =1+1）</text>
            <view class="detail-amount-wrap">
              <input class="detail-amount" v-model="row.amount" type="text" confirm-type="done" placeholder="输入数字或 =公式" :adjust-position="true" @input="onDetailInput(row)" />
              <view class="amount-fx" @click.stop="handleFxTap(row)"><text class="fx-text">计算</text></view>
            </view>
            <view class="quick-chips" style="margin-top:6rpx"><view v-for="v in [100,500,1000,5000]" :key="v" class="chip chip--sm" @click="row.amount=String(v);onDetailInput(row)"><text class="chip-text">{{ v }}</text></view><view class="chip chip--sm chip--fx" @click="handleFxChip(row)"><text class="chip-text">fx</text></view></view>
          </view>
        </view>
        <view class="detail-row">
          <view class="detail-col"><text class="detail-label">变化量</text><text class="detail-val">{{ row.changeAmount }}</text></view>
          <view class="detail-col" style="flex:1"><input class="detail-remark" v-model="row.remark" placeholder="备注" /></view>
        </view>
      </view>
      <view v-if="detailList.length===0" class="empty-detail"><text class="empty-detail-text">暂无明细，点击添加</text></view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-bar">
      <button class="submit-btn" type="primary" @click="submit" :loading="submitting" :disabled="submitting">保存</button>
      <button v-if="!isAdd" class="delete-btn" @click="doDelete" :disabled="submitting">删除</button>
    </view>
  </view>
</template>

<script>
import { getSummary, addSummary, updateSummary, delSummary, initSummary, getDictData } from '@/api/main/summary'
import themeMixin from '@/mixins/theme'

export default {
  mixins: [themeMixin],
  data() {
    return {
      isAdd: true,
      submitting: false,
      yearMonth: '',
      form: { id:null, year:null, month:null, remark:'', income:'0', expense:'0.00', balance:'0.00', startDeposit:'0', endDeposit:'0' },
      detailList: [],
      checkedIdx: [],
      ckOptions: [],
      szOptions: [],
      quickAmounts: [5000,10000,20000,30000]
    }
  },
  onLoad(options) {
    this.loadDicts()
    if (options.id) { this.isAdd=false; this.loadDetail(options.id) }
    else { this.isAdd=true; this.loadInit() }
  },
  methods: {
    loadDicts() {
      getDictData('cktype').then(res=>{ const list=res.data||[]; this.ckOptions=list.map(i=>({value:String(i.dictValue), label:i.dictLabel})) }).catch(()=>{ this.ckOptions=[{value:'1',label:'银行卡'},{value:'2',label:'外币'}] })
      getDictData('sztype').then(res=>{ const list=res.data||[]; this.szOptions=list.map(i=>({value:String(i.dictValue), label:i.dictLabel})) }).catch(()=>{ this.szOptions=[{value:'1',label:'存款'}] })
    },
    loadInit() {
      initSummary().then(res=>{
        const d=res.data||{}
        const head=d.head||{}
        const items=d.items||[]
        this.form.startDeposit = head.endDeposit!=null?String(head.endDeposit):'0'
        let date=new Date(head.year, (head.month||1)-1)
        date.setMonth(date.getMonth()+1)
        this.form.year = date.getFullYear()
        this.form.month = date.getMonth()+1
        this.yearMonth = `${this.form.year}-${String(this.form.month).padStart(2,'0')}`
        items.forEach((detail,idx)=>{
          detail.index=idx+1
          detail.summaryId=null
          detail.id=null
          detail.changeAmountOld = (detail.amount||0)*-1
          detail.changeAmount = detail.changeAmountOld
          detail.amount='0'
          detail.amount=String(detail.amount)
        })
        this.detailList = items.map(i=>({ category:String(i.category||''), type:String(i.type||''), amount:String(i.amount||'0'), changeAmount: i.changeAmount!=null?String(i.changeAmount):'0', changeAmountOld:i.changeAmountOld, name:i.name||'', remark:i.remark||'' }))
        this.recalc()
      }).catch(()=>{ const now=new Date(); this.form.year=now.getFullYear(); this.form.month=now.getMonth()+1; this.yearMonth=`${this.form.year}-${String(this.form.month).padStart(2,'0')}` })
    },
    reloadInit(){ this.loadInit(); uni.showToast({title:'已重置为上月模板', icon:'none'}) },
    loadDetail(id){ uni.showLoading({title:'加载中...'}); getSummary(id).then(res=>{
      const d=res.data||{}
      this.form={ id:d.id, year:d.year, month:d.month, remark:d.remark||'', income:String(d.income||0), expense:String(d.expense||0), balance:String(d.balance||0), startDeposit:String(d.startDeposit||0), endDeposit:String(d.endDeposit||0) }
      this.yearMonth=`${d.year}-${String(d.month).padStart(2,'0')}`
      const list=d.hsmDetailList||[]
      list.forEach(item=>{ if(item.changeAmountOld==null) item.changeAmountOld=item.changeAmount||0 })
      this.detailList=list.map(i=>({ id:i.id, summaryId:i.summaryId, category:String(i.category||''), type:String(i.type||''), amount:String(i.amount||0), changeAmount:String(i.changeAmount||0), changeAmountOld:i.changeAmountOld||0, name:i.name||'', remark:i.remark||'' }))
      uni.hideLoading()
    }).catch(()=>{ uni.hideLoading(); uni.showToast({title:'加载失败', icon:'error'}); setTimeout(()=>uni.navigateBack(),1200) }) },
    onYearMonthChange(e){ this.yearMonth=e.detail.value; const [y,m]=this.yearMonth.split('-'); this.form.year=parseInt(y); this.form.month=parseInt(m) },
    catIndex(v){ const idx=this.ckOptions.findIndex(o=>o.value===String(v)); return idx>=0?idx:0 },
    typeIndex(v){ const idx=this.szOptions.findIndex(o=>o.value===String(v)); return idx>=0?idx:0 },
    catLabel(v){ const f=this.ckOptions.find(o=>o.value===String(v)); return f?f.label:'请选择' },
    typeLabel(v){ const f=this.szOptions.find(o=>o.value===String(v)); return f?f.label:'请选择' },
    handleAddDetail(){ this.detailList.push({ category:this.ckOptions[0]?.value||'1', type:this.szOptions[0]?.value||'1', amount:'0', changeAmount:'0', changeAmountOld:0, name:'', remark:'' }); this.recalc() },
    handleDelDetail(){ if(this.checkedIdx.length===0){ uni.showToast({title:'先勾选要删除的行', icon:'none'}); return } this.detailList=this.detailList.filter((_,i)=>!this.checkedIdx.includes(i)); this.checkedIdx=[]; this.recalc() },
    toggleCheck(idx){ const p=this.checkedIdx.indexOf(idx); if(p>=0) this.checkedIdx.splice(p,1); else this.checkedIdx.push(idx) },
    onIncomeInput(){ this.recalc() },
    onDetailInput(row){
      const sum=this.sumAmount(this.detailList)
      this.form.endDeposit=sum
      this.form.balance=this.sub(sum,this.form.startDeposit)
      this.calcExpense()
      this.detailList.forEach((item, i)=>{
        if(item===row){ const base=item.changeAmountOld||0; item.changeAmount = (parseFloat(base)+(parseFloat(row.amount)||0)).toFixed(2) }
      })
    },
    handleFxTap(row){
      // 右侧计算按钮：显式触发，不受键盘影响 add by CYQ 2026-08-31
      const r=this.calcFormula(row.amount)
      if(r!==null){ row.amount=String(r); this.onDetailInput(row); uni.showToast({title:'已计算: '+r, icon:'none'}) }
      else if(row.amount && row.amount.startsWith('=')) uni.showToast({title:'公式有误', icon:'none'})
      else uni.showToast({title:'请输入 =公式', icon:'none'})
    },
    handleFxChip(row){
      if(!row.amount) row.amount='='
      else if(!row.amount.startsWith('=')) row.amount='='+row.amount
      // 聚焦到输入框，便于继续输入
      this.$forceUpdate()
    },
    onFormulaEnter(row){
      if(row.amount && typeof row.amount==='string' && row.amount.trim().startsWith('=')){
        const r=this.calcFormula(row.amount.trim())
        if(r!==null){ row.amount=String(r); this.onDetailInput(row) }
        else uni.showToast({title:'公式有误', icon:'none'})
      }
    },
    calcFormula(formula){
      if(!formula || !formula.trim().startsWith('=')) return null
      let expr=formula.trim().substring(1).trim()
      if(!expr) return null
      // 替换 A1/A2 引用为数值（微信小程序禁用 Function/eval，需安全解析）
      expr=expr.replace(/A(\d+)/g,(m,idx)=>{ const i=parseInt(idx)-1; const v=this.detailList[i]?.amount; const n=parseFloat(v); return isNaN(n)?'0':String(n) })
      // 仅允许数字、运算符、括号、空格和小数点，防止注入
      if(!/^[0-9+\-*/().\s]+$/.test(expr)) return null
      try{
        const val=this.safeEval(expr)
        return val==null || isNaN(val) ? null : parseFloat(Number(val).toFixed(2))
      }catch(e){ return null }
    },
    // 安全四则运算（支持 + - * / 括号，小数），不使用 eval/Function，兼容微信小程序
    safeEval(expr){
      expr=expr.replace(/\s+/g,'')
      if(!expr) return null
      // 处理前导负号：-1+2 → 0-1+2
      if(expr[0]==='-') expr='0'+expr
      expr=expr.replace(/\( -/g,'(0-')
      const ops=[]
      const vals=[]
      const prec=o=> o==='+'||o==='-'?1: o==='*'||o==='/'?2:0
      const apply=()=>{
        const op=ops.pop()
        const b=vals.pop()
        const a=vals.pop()
        if(a==null||b==null) throw new Error('bad')
        let r=0
        if(op==='+') r=a+b
        else if(op==='-') r=a-b
        else if(op==='*') r=a*b
        else if(op==='/'){ if(b===0) throw new Error('div0'); r=a/b }
        vals.push(r)
      }
      let i=0
      while(i<expr.length){
        const c=expr[i]
        if((c>='0'&&c<='9')||c==='.'){
          let j=i
          while(j<expr.length && ((expr[j]>='0'&&expr[j]<='9')||expr[j]==='.')) j++
          const numStr=expr.slice(i,j)
          if((numStr.match(/\./g)||[]).length>1) throw new Error('num')
          const n=parseFloat(numStr)
          if(isNaN(n)) throw new Error('num')
          vals.push(n)
          i=j
          continue
        } else if(c==='('){
          ops.push(c); i++
        } else if(c===')'){
          while(ops.length && ops[ops.length-1]!=='(') apply()
          if(!ops.length) throw new Error('paren')
          ops.pop()
          i++
        } else if('+-*/'.includes(c)){
          // 处理连续运算符中的单目负号：1*-2 → 1*(0-2)
          if(c==='-' && (i===0 || '+-*/('.includes(expr[i-1]))){
            vals.push(0)
          }
          while(ops.length && ops[ops.length-1]!=='(' && prec(ops[ops.length-1])>=prec(c)) apply()
          ops.push(c); i++
        } else throw new Error('char')
      }
      while(ops.length){
        if(ops[ops.length-1]==='(') throw new Error('paren')
        apply()
      }
      if(vals.length!==1) throw new Error('eval')
      return vals[0]
    },
    recalc(){
      const sum=this.sumAmount(this.detailList)
      this.form.endDeposit=sum
      this.form.balance=this.sub(sum,this.form.startDeposit)
      this.calcExpense()
      this.detailList.forEach(item=>{ const base=item.changeAmountOld||0; item.changeAmount=(parseFloat(base)+(parseFloat(item.amount)||0)).toFixed(2) })
    },
    sumAmount(arr){
      const total=arr.reduce((acc,cur)=>{ const n=Number(cur.amount); return isNaN(n)?acc:acc+n },0)
      return total.toFixed(2)
    },
    sub(a,b){ const n1=Number(a), n2=Number(b); if(isNaN(n1)||isNaN(n2)) return '0.00'; return (n1-n2).toFixed(2) },
    calcExpense(){
      const income=parseFloat(this.form.income)||0
      const balance=parseFloat(this.form.balance)||0
      const expense=income-balance
      this.form.expense = expense>=0?expense.toFixed(2):'0.00'
      if(expense<0) uni.showToast({title:'支出为负，请检查', icon:'none'})
    },
    submit(){
      if(!this.yearMonth){ uni.showToast({title:'请选择年月', icon:'none'}); return }
      const income=parseFloat(this.form.income)||0
      if(income<=0){ uni.showToast({title:'收入不能为0', icon:'none'}); return }
      this.calcExpense()
      this.submitting=true
      const data={ id:this.form.id, year:this.form.year, month:this.form.month, remark:this.form.remark||null, income:parseFloat(this.form.income)||0, expense:parseFloat(this.form.expense)||0, balance:parseFloat(this.form.balance)||0, startDeposit:parseFloat(this.form.startDeposit)||0, endDeposit:parseFloat(this.form.endDeposit)||0, hsmDetailList: this.detailList.map(d=>({ id:d.id||null, summaryId:d.summaryId||null, category:d.category, type:d.type, amount:parseFloat(d.amount)||0, changeAmount:parseFloat(d.changeAmount)||0, name:d.name||null, remark:d.remark||null })) }
      const req=this.isAdd?addSummary(data):updateSummary(data)
      req.then(()=>{ uni.showToast({title:this.isAdd?'新增成功':'修改成功', icon:'success'}); setTimeout(()=>uni.navigateBack(),800) }).catch(()=>{ uni.showToast({title:'保存失败', icon:'none'}) }).finally(()=>{ this.submitting=false })
    },
    doDelete(){ uni.showModal({title:'确认删除', content:'确定删除该汇总吗？', confirmColor:'#F56C6C', success:(res)=>{ if(res.confirm) delSummary(this.form.id).then(()=>{ uni.showToast({title:'已删除', icon:'success'}); setTimeout(()=>uni.navigateBack(),800) }) }}) }
  }
}
</script>

<style scoped>
.form-container { min-height:100vh; background: var(--bg-gradient, #F8F9FB); padding-bottom:120rpx; }
.init-tip { margin:16rpx 28rpx; background: var(--card-bg); border:1rpx solid var(--card-border); border-radius:16rpx; padding:16rpx 20rpx; display:flex; align-items:center; gap:10rpx; box-shadow: var(--card-shadow); }
.init-tip-text { flex:1; font-size:24rpx; color: var(--color-text); }
.init-tip-btn { font-size:24rpx; color: var(--color-primary); font-weight:700; }
.form-section { padding:0 28rpx; }
.form-group { background: var(--card-bg); border-radius:20rpx; padding:24rpx; margin-bottom:16rpx; box-shadow: var(--card-shadow); border:1rpx solid var(--card-border); }
.form-label { display:block; font-size:26rpx; font-weight:600; color: var(--color-text-secondary); margin-bottom:14rpx; }
.required { color:#F53F3F; margin-left:4rpx; }
.picker-input { display:flex; align-items:center; font-size:28rpx; color: var(--color-text); padding:10rpx 0; }
.picker-input.placeholder { color:#C9CDD4; }
.amount-input-wrap { display:flex; align-items:center; gap:10rpx; background: var(--skeleton-1); border-radius: var(--radius-card, 16rpx); padding:14rpx 20rpx; min-height:88rpx; box-sizing:border-box; border:1rpx solid var(--card-border); }
.currency-sign { font-size:36rpx; font-weight:800; color: var(--color-text); line-height:48rpx; flex-shrink:0; }
.amount-input { flex:1; font-size:36rpx; font-weight:800; color: var(--color-text); border:none; background:transparent; line-height:48rpx; min-height:48rpx; box-sizing:border-box; padding:0; margin:0; }
.quick-chips { display:flex; gap:12rpx; margin-top:14rpx; flex-wrap:wrap; }
.chip { padding:10rpx 20rpx; border-radius:40rpx; background: var(--skeleton-2); border:1rpx solid var(--card-border); }
.chip--sm { padding:8rpx 16rpx; }
.chip-text { font-size:24rpx; color: var(--color-text); font-weight:600; }
.auto-grid { display:flex; gap:12rpx; margin-bottom:16rpx; }
.auto-item { flex:1; background: var(--card-bg); border-radius:16rpx; padding:16rpx; text-align:center; box-shadow: var(--card-shadow); border:1rpx solid var(--card-border); }
.auto-label { display:block; font-size:22rpx; color: var(--color-text-secondary); margin-bottom:6rpx; }
.auto-val { font-size:28rpx; font-weight:800; color: var(--color-text); }
.auto-val--bold { color: var(--color-primary); }
.detail-header { display:flex; justify-content:space-between; align-items:center; margin:20rpx 0 12rpx; }
.detail-title { font-size:30rpx; font-weight:800; color: var(--color-text); }
.detail-actions { display:flex; gap:12rpx; }
.detail-btn { display:flex; align-items:center; gap:6rpx; padding:10rpx 16rpx; border-radius:40rpx; }
.detail-btn--add { background: var(--color-primary); }
.detail-btn--del { background: var(--card-bg); border:1rpx solid #F56C6C; }
.detail-btn-text { font-size:24rpx; color:#fff; font-weight:600; }
.detail-card { background: var(--card-bg); border-radius:20rpx; padding:20rpx; margin-bottom:16rpx; box-shadow: var(--card-shadow); border:2rpx solid var(--card-border); }
.detail-card--checked { border-color: var(--color-primary); }
.detail-top { display:flex; align-items:center; gap:12rpx; margin-bottom:12rpx; }
.check-box { width:36rpx; height:36rpx; border-radius:50%; border:2rpx solid #C0C4CC; display:flex; align-items:center; justify-content:center; }
.check-box--on { background: var(--color-primary); border-color: var(--color-primary); }
.detail-index { font-size:24rpx; color: var(--color-text-secondary); font-weight:700; }
.detail-name { flex:1; font-size:26rpx; color: var(--color-text); background: var(--skeleton-1); border-radius: var(--radius-card, 12rpx); padding:12rpx 16rpx; min-height:48rpx; line-height:32rpx; box-sizing:border-box; border:1rpx solid var(--card-border); }
.detail-row { display:flex; gap:16rpx; margin-bottom:12rpx; }
.detail-col { flex:1; display:flex; flex-direction:column; gap:8rpx; }
.detail-label { font-size:22rpx; color: var(--color-text-secondary); }
.detail-picker { display:flex; align-items:center; justify-content:space-between; background: var(--skeleton-1); border-radius: var(--radius-card, 12rpx); padding:12rpx 16rpx; min-height:64rpx; box-sizing:border-box; border:1rpx solid var(--card-border); }
.picker-text { font-size:26rpx; color: var(--color-text); line-height:36rpx; }
.detail-amount-wrap { display:flex; align-items:center; gap:10rpx; background: var(--skeleton-1); border-radius: var(--radius-card, 12rpx); padding:10rpx 16rpx; min-height:72rpx; box-sizing:border-box; border:1rpx solid var(--card-border); }
.detail-amount { flex:1; font-size:28rpx; font-weight:700; color: var(--color-text); background:transparent; border:none; line-height:40rpx; min-height:40rpx; padding:0; margin:0; box-sizing:border-box; }
.amount-fx { min-width:80rpx; height:52rpx; border-radius:26rpx; background: var(--color-primary); display:flex; align-items:center; justify-content:center; flex-shrink:0; padding:0 12rpx; box-shadow:0 2rpx 8rpx var(--color-primary-dim); }
.amount-fx--active { background: var(--color-primary); }
.fx-text { font-size:22rpx; color:#fff; font-weight:800; line-height:1; white-space:nowrap; }
.chip--fx { background: var(--color-primary) !important; border-color: var(--color-primary) !important; }
.chip--fx .chip-text { color:#fff !important; }
.detail-val { font-size:26rpx; color: var(--color-text); font-weight:700; background: var(--skeleton-1); border-radius: var(--radius-card, 12rpx); padding:12rpx 16rpx; min-height:48rpx; line-height:32rpx; box-sizing:border-box; border:1rpx solid var(--card-border); }
.detail-remark { font-size:26rpx; color: var(--color-text); background: var(--skeleton-1); border-radius: var(--radius-card, 12rpx); padding:12rpx 16rpx; min-height:48rpx; line-height:32rpx; box-sizing:border-box; border:1rpx solid var(--card-border); }
.empty-detail { text-align:center; padding:40rpx; color: var(--color-text-secondary); font-size:26rpx; }
.action-bar { position:fixed; bottom:0; left:0; right:0; display:flex; gap:16rpx; padding:20rpx 28rpx; padding-bottom:calc(20rpx + env(safe-area-inset-bottom)); background: var(--card-bg); box-shadow:0 -4rpx 16rpx rgba(0,0,0,0.06); }
.submit-btn { flex:1; height:88rpx; border-radius:44rpx; background: var(--color-primary); color:#fff; font-size:30rpx; font-weight:600; border:none; }
.delete-btn { width:160rpx; height:88rpx; border-radius:44rpx; background: var(--card-bg); color:#F56C6C; font-size:28rpx; border:2rpx solid #F56C6C; }
.empty-detail-text { color: var(--color-text-secondary); }
</style>
