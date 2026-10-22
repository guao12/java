<script setup>
import { computed, ref } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  type: { type: String, default: 'text' }, // text | password | email | select | tel
  placeholder: { type: String, default: '' },
  icon: { type: String, default: '' },
  error: { type: String, default: '' },
  hint: { type: String, default: '' },
  valid: { type: Boolean, default: false },
  optional: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  autocomplete: { type: String, default: 'off' },
  autofocus: { type: Boolean, default: false },
  maxlength: { type: [String, Number], default: undefined }
})

const emit = defineEmits(['update:modelValue', 'blur', 'enter'])

const focused = ref(false)
const revealed = ref(false)

const inputType = computed(() => {
  if (props.type !== 'password') return props.type
  return revealed.value ? 'text' : 'password'
})

function onInput(e) {
  emit('update:modelValue', e.target.value)
}

function onEnter() {
  emit('enter')
}
</script>

<template>
  <div class="field">
    <label v-if="label" class="field__label" for="">
      <span>{{ label }}</span>
      <span v-if="optional" class="optional">{{ optional }}</span>
    </label>

    <div
      class="field__control"
      :class="{ 'is-focus': focused, 'is-error': !!error, 'is-ok': valid && !error }"
    >
      <span v-if="icon" class="field__icon"><AppIcon :name="icon" :size="16" /></span>

      <select
        v-if="type === 'select'"
        :value="modelValue"
        :autocomplete="autocomplete"
        @change="onInput"
        @focus="focused = true"
        @blur="focused = false"
      >
        <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
        <option v-for="opt in options" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>

      <input
        v-else
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        :autofocus="autofocus"
        :maxlength="maxlength"
        spellcheck="false"
        @input="onInput"
        @focus="focused = true"
        @blur="focused = false; emit('blur')"
        @keyup.enter="onEnter"
      />

      <span v-if="type === 'select'" class="field__arrow"><AppIcon name="chevron" :size="15" /></span>

      <button
        v-else-if="type === 'password'"
        class="field__toggle"
        type="button"
        :aria-label="revealed ? '隐藏密码' : '显示密码'"
        @click="revealed = !revealed"
      >
        <AppIcon :name="revealed ? 'eye-off' : 'eye'" :size="16" />
      </button>
    </div>

    <p v-if="error" class="field__msg"><AppIcon name="alert" :size="13" />{{ error }}</p>
    <p v-else-if="hint" class="field__msg hint">{{ hint }}</p>
  </div>
</template>
