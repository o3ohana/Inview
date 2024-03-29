<template>
  <el-dialog
    v-model="openDialog"
    title="방 만들기"
    :width="windowWidth > 416 ? 416 : windowWidth"
  >
    <el-form 
      ref="ruleFormRef"
      :model="ruleForm"
      :rules="rules"
      label-width="90px"
      class="demo-ruleForm"
      size="large"
    >
      <el-form-item label="제목" prop="title">
        <el-input v-model="ruleForm.title" placeholder="제목을 입력해주세요"></el-input>
      </el-form-item>
      <el-form-item label="직군" prop="industryName">
        <IndustrySearchBar v-model="ruleForm.industryName" />
      </el-form-item>
      <el-form-item label="회사">
        <CompanySearchBar v-model="companyName" />
      </el-form-item>
      <el-form-item label="참가 인원">
        <el-radio-group v-model="ruleForm.userLimit">
          <el-radio-button label="1"></el-radio-button>
          <el-radio-button label="2"></el-radio-button>
          <el-radio-button label="3"></el-radio-button>
          <el-radio-button label="4"></el-radio-button>
          <el-radio-button label="5"></el-radio-button>
          <el-radio-button label="6"></el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="시작 시간" prop="startTime">
        <el-date-picker
          v-model="ruleForm.startTime"
          type="datetime"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="날짜"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="종료 시간" prop="endTime">
        <el-date-picker
          v-model="ruleForm.endTime"
          type="datetime"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="날짜"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="비밀번호">
        <el-input 
          v-model="ruleForm.password" 
          placeholder="비밀번호 입력해주세요" 
          autocomplete="off"
        ></el-input>
      </el-form-item>
      <div class="d-flex justify-content-center">
        <el-button type="primary" @click="submitForm(ruleFormRef)">생성</el-button>
        <el-button @click="openDialog=false">취소</el-button>
      </div>
    </el-form>
  </el-dialog>
</template>

<script lang="ts">
import { defineComponent, reactive, ref, computed, onMounted, onUnmounted } from 'vue'
import CompanySearchBar from '@/components/SearchFilterBar/CompanySearchBar.vue'
import IndustrySearchBar from '@/components/SearchFilterBar/IndustrySearchBar.vue'
import type { ElForm } from 'element-plus'
import axios from 'axios'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'CreateMeeting',
  props: {
    modelValue: Boolean,
  },
  components: {
    CompanySearchBar, IndustrySearchBar
  },
  setup(props, { emit }) {
    const openDialog = computed({
      get: () => props.modelValue,
      set: (value) => emit("update:modelValue", value),
    });
    const ruleFormRef = ref<InstanceType<typeof ElForm>>()
    const companyName = ref('')
    const ruleForm = reactive({
      title: '',
      industryName: '',
      companyNameList: [],
      userLimit: 6,
      startTime: '',
      endTime: '',
      password: ''
    })

    // eslint-disable-next-line
    const validateStartTime = (rule: any, value: any, callback: any) => {
      if (ruleForm.startTime === '') {
        callback()
      } else if (dayjs().isAfter(ruleForm.startTime, "minute")) {
        callback(new Error('시작 시간은 현재 시간 이후로 설정해주세요'))
      } else {
        callback()
      }
    }

    // eslint-disable-next-line
    const validateEndTime = (rule: any, value: any, callback: any) => {
      if (ruleForm.endTime === '') {
        callback()
      } else if (ruleForm.startTime && dayjs(ruleForm.endTime).isBefore(ruleForm.startTime)) {
        callback(new Error('종료 시간은 시작 시간 이후로 설정해주세요'))
      } else if (dayjs().isAfter(ruleForm.endTime, "minute")) {
        callback(new Error('종료 시간은 현재 시간 이후로 설정해주세요'))
      } else {
        callback()
      }
    }

    const rules = reactive({
      title: [
        {
          required: true,
          message: '제목을 입력해주세요',
          trigger: 'blur',
        },
        {
          min: 1,
          max: 20,
          message: '제목은 1자 이상 20자 이하 입력해주세요',
          trigger: 'blur',
        },
      ],
      industryName: [
        {
          required: true,
          message: '직군을 선택해주세요',
          trigger: 'blur',
        },
      ],
      startTime: [
        { 
          validator: validateStartTime,
          trigger: "blur",
        }
      ],
      endTime: [
        { 
          validator: validateEndTime,
          trigger: "blur",
        }
      ],
    })
    const store = useStore()
    const router = useRouter()

    interface createMeetingBody {
      title: string
      industryName: string
      companyNameList: string[],
      userLimit: number
      startTime: string
      endTime: string
      password: string
    }

    const createMeeting = function (body: createMeetingBody) {
      axios.post(
        '/meeting', 
        body,
        { headers: { Authorization: `Bearer ${localStorage.getItem("token")}` }
        }).then(res => {
          openDialog.value = false
          store.dispatch('setMeeting', res.data.data.id)
          store.dispatch('setParticipants', [{id: store.state.user.id, nickname: store.state.user.nickname, ready: true}])
          router.push({ name: 'Meeting', params: { meetingUrl: res.data.data.url } })
        }).catch(err => {
        })
    }
    const submitForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
      if (!formEl) return
      formEl.validate((valid) => {
        if (valid) {
          const { title, industryName, userLimit, startTime, endTime, password } = ruleForm
          const companyNameList = companyName.value ? [companyName.value] : []
          createMeeting({
            title,
            industryName,
            companyNameList: companyNameList,
            userLimit,
            startTime,
            endTime,
            password
          })
        } else {
          return false
        }
      })
    }
    const windowWidth = ref(450)
		onMounted(() => {
			window.addEventListener('resize', function () {
				windowWidth.value = window.innerWidth
			})
		})
    onUnmounted(() => {
      window.removeEventListener('resize', function () {
				windowWidth.value = window.innerWidth
			})
    })
    return {companyName, ruleFormRef, ruleForm, rules, submitForm, validateStartTime, openDialog, windowWidth }
  }
})
</script>

<style>

</style>