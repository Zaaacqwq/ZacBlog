<template>
  <div class="right-board">
    <el-tabs v-model="currentTab" class="center-tabs">
      <el-tab-pane label="Components Properties" name="field" />
      <el-tab-pane label="Form Properties" name="form" />
    </el-tabs>
    <div class="field-box">
      <a class="document-link" target="_blank" :href="documentLink" title="View Component Documentation">
        <i class="el-icon-link" />
      </a>
      <el-scrollbar class="right-scrollbar">
        <!-- 组件属性 -->
        <el-form v-show="currentTab==='field' && showField" size="small" label-width="90px">
          <el-form-item v-if="activeData.changeTag" label="Component Type">
            <el-select
              v-model="activeData.tagIcon"
              placeholder="Select Component Type"
              :style="{width: '100%'}"
              @change="tagChange"
            >
              <el-option-group v-for="group in tagList" :key="group.label" :label="group.label">
                <el-option
                  v-for="item in group.options"
                  :key="item.label"
                  :label="item.label"
                  :value="item.tagIcon"
                >
                  <svg-icon class="node-icon" :icon-class="item.tagIcon" />
                  <span> {{ item.label }}</span>
                </el-option>
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.vModel!==undefined" label="Field Name">
            <el-input v-model="activeData.vModel" placeholder="Enter field name (v-model)" />
          </el-form-item>
          <el-form-item v-if="activeData.componentName!==undefined" label="Component Name">
            {{ activeData.componentName }}
          </el-form-item>
          <el-form-item v-if="activeData.label!==undefined" label="Label">
            <el-input v-model="activeData.label" placeholder="Enter label" />
          </el-form-item>
          <el-form-item v-if="activeData.placeholder!==undefined" label="Placeholder">
            <el-input v-model="activeData.placeholder" placeholder="Enter placeholder" />
          </el-form-item>
          <el-form-item v-if="activeData['start-placeholder']!==undefined" label="Start Placeholder">
            <el-input v-model="activeData['start-placeholder']" placeholder="Enter start placeholder" />
          </el-form-item>
          <el-form-item v-if="activeData['end-placeholder']!==undefined" label="End Placeholder">
            <el-input v-model="activeData['end-placeholder']" placeholder="Enter end placeholder" />
          </el-form-item>
          <el-form-item v-if="activeData.span!==undefined" label="Form Grid">
            <el-slider v-model="activeData.span" :max="24" :min="1" :marks="{12:''}" @change="spanChange" />
          </el-form-item>
          <el-form-item v-if="activeData.layout==='rowFormItem'" label="Grid Gap">
            <el-input-number v-model="activeData.gutter" :min="0" placeholder="Grid Gap" />
          </el-form-item>
          <el-form-item v-if="activeData.layout==='rowFormItem'" label="Layout Mode">
            <el-radio-group v-model="activeData.type">
              <el-radio-button label="default" />
              <el-radio-button label="flex" />
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData.justify!==undefined&&activeData.type==='flex'" label="Horizontal Alignment">
            <el-select v-model="activeData.justify" placeholder="Select horizontal alignment" :style="{width: '100%'}">
              <el-option
                v-for="(item, index) in justifyOptions"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.align!==undefined&&activeData.type==='flex'" label="Vertical Alignment">
            <el-radio-group v-model="activeData.align">
              <el-radio-button label="top" />
              <el-radio-button label="middle" />
              <el-radio-button label="bottom" />
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData.labelWidth!==undefined" label="Label Width">
            <el-input v-model.number="activeData.labelWidth" type="number" placeholder="Enter label width" />
          </el-form-item>
          <el-form-item v-if="activeData.style&&activeData.style.width!==undefined" label="Component Width">
            <el-input v-model="activeData.style.width" placeholder="Enter component width" clearable />
          </el-form-item>
          <el-form-item v-if="activeData.vModel!==undefined" label="Default Value">
            <el-input
              :value="setDefaultValue(activeData.defaultValue)"
              placeholder="Enter default value"
              @input="onDefaultValueInput"
            />
          </el-form-item>
          <el-form-item v-if="activeData.tag==='el-checkbox-group'" label="Min Selection">
            <el-input-number
              :value="activeData.min"
              :min="0"
              placeholder="Minimum selection"
              @input="$set(activeData, 'min', $event?$event:undefined)"
            />
          </el-form-item>
          <el-form-item v-if="activeData.tag==='el-checkbox-group'" label="Max Selection">
            <el-input-number
              :value="activeData.max"
              :min="0"
              placeholder="Maximum selection"
              @input="$set(activeData, 'max', $event?$event:undefined)"
            />
          </el-form-item>
          <el-form-item v-if="activeData.prepend!==undefined" label="Prefix">
            <el-input v-model="activeData.prepend" placeholder="Enter prefix" />
          </el-form-item>
          <el-form-item v-if="activeData.append!==undefined" label="Suffix">
            <el-input v-model="activeData.append" placeholder="Enter suffix" />
          </el-form-item>
          <el-form-item v-if="activeData['prefix-icon']!==undefined" label="Prefix Icon">
            <el-input v-model="activeData['prefix-icon']" placeholder="Enter prefix icon name">
              <el-button slot="append" icon="el-icon-thumb" @click="openIconsDialog('prefix-icon')">
                Select
              </el-button>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData['suffix-icon'] !== undefined" label="Suffix Icon">
            <el-input v-model="activeData['suffix-icon']" placeholder="Enter suffix icon name">
              <el-button slot="append" icon="el-icon-thumb" @click="openIconsDialog('suffix-icon')">
                Select
              </el-button>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="Option Separator">
            <el-input v-model="activeData.separator" placeholder="Enter option separator" />
          </el-form-item>
          <el-form-item v-if="activeData.autosize !== undefined" label="Min Rows">
            <el-input-number v-model="activeData.autosize.minRows" :min="1" placeholder="Min rows" />
          </el-form-item>
          <el-form-item v-if="activeData.autosize !== undefined" label="Max Rows">
            <el-input-number v-model="activeData.autosize.maxRows" :min="1" placeholder="Max rows" />
          </el-form-item>
          <el-form-item v-if="activeData.min !== undefined" label="Min Value">
            <el-input-number v-model="activeData.min" placeholder="Min Value" />
          </el-form-item>
          <el-form-item v-if="activeData.max !== undefined" label="Max Value">
            <el-input-number v-model="activeData.max" placeholder="Max Value" />
          </el-form-item>
          <el-form-item v-if="activeData.step !== undefined" label="Step">
            <el-input-number v-model="activeData.step" placeholder="Step" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="Precision">
            <el-input-number v-model="activeData.precision" :min="0" placeholder="Precision" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="Button Position">
            <el-radio-group v-model="activeData['controls-position']">
              <el-radio-button label="">
                Default
              </el-radio-button>
              <el-radio-button label="right">
                Right
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData.maxlength !== undefined" label="Max Input">
            <el-input v-model="activeData.maxlength" placeholder="Enter max characters">
              <template slot="append">
                characters
              </template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData['active-text'] !== undefined" label="Active Text">
            <el-input v-model="activeData['active-text']" placeholder="Enter active text" />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-text'] !== undefined" label="Inactive Text">
            <el-input v-model="activeData['inactive-text']" placeholder="Enter inactive text" />
          </el-form-item>
          <el-form-item v-if="activeData['active-value'] !== undefined" label="Active Value">
            <el-input
              :value="setDefaultValue(activeData['active-value'])"
              placeholder="Enter active value"
              @input="onSwitchValueInput($event, 'active-value')"
            />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-value'] !== undefined" label="Inactive Value">
            <el-input
              :value="setDefaultValue(activeData['inactive-value'])"
              placeholder="Enter inactive value"
              @input="onSwitchValueInput($event, 'inactive-value')"
            />
          </el-form-item>
          <el-form-item
            v-if="activeData.type !== undefined && 'el-date-picker' === activeData.tag"
            label="Date Type"
          >
            <el-select
              v-model="activeData.type"
              placeholder="Select date type"
              :style="{ width: '100%' }"
              @change="dateTypeChange"
            >
              <el-option
                v-for="(item, index) in dateOptions"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.name !== undefined" label="File Field Name">
            <el-input v-model="activeData.name" placeholder="Enter the upload file field name" />
          </el-form-item>
          <el-form-item v-if="activeData.accept !== undefined" label="File Type">
            <el-select
              v-model="activeData.accept"
              placeholder="Select file type"
              :style="{ width: '100%' }"
              clearable
            >
              <el-option label="Image" value="image/*" />
              <el-option label="Video" value="video/*" />
              <el-option label="Audio" value="audio/*" />
              <el-option label="excel" value=".xls,.xlsx" />
              <el-option label="word" value=".doc,.docx" />
              <el-option label="pdf" value=".pdf" />
              <el-option label="txt" value=".txt" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.fileSize !== undefined" label="File Size">
            <el-input v-model.number="activeData.fileSize" placeholder="Enter file size">
              <el-select slot="append" v-model="activeData.sizeUnit" :style="{ width: '66px' }">
                <el-option label="KB" value="KB" />
                <el-option label="MB" value="MB" />
                <el-option label="GB" value="GB" />
              </el-select>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData.action !== undefined" label="Upload URL">
            <el-input v-model="activeData.action" placeholder="Enter upload URL" clearable />
          </el-form-item>
          <el-form-item v-if="activeData['list-type'] !== undefined" label="List Type">
            <el-radio-group v-model="activeData['list-type']" size="small">
              <el-radio-button label="text">
                text
              </el-radio-button>
              <el-radio-button label="picture">
                picture
              </el-radio-button>
              <el-radio-button label="picture-card">
                picture-card
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="activeData.buttonText !== undefined"
            v-show="'picture-card' !== activeData['list-type']"
            label="Button Text"
          >
            <el-input v-model="activeData.buttonText" placeholder="Please enter button text" />
          </el-form-item>
          <el-form-item v-if="activeData['range-separator'] !== undefined" label="Separator">
            <el-input v-model="activeData['range-separator']" placeholder="Please enter separator" />
          </el-form-item>
          <el-form-item v-if="activeData['picker-options'] !== undefined" label="Time Range">
            <el-input
              v-model="activeData['picker-options'].selectableRange"
              placeholder="Please enter time range"
            />
          </el-form-item>
          <el-form-item v-if="activeData.format !== undefined" label="Time Format">
            <el-input
              :value="activeData.format"
              placeholder="Please enter time format"
              @input="setTimeValue($event)"
            />
          </el-form-item>
          <template v-if="['el-checkbox-group', 'el-radio-group', 'el-select'].indexOf(activeData.tag) > -1">
            <el-divider>Options</el-divider>
            <draggable
              :list="activeData.options"
              :animation="340"
              group="selectItem"
              handle=".option-drag"
            >
              <div v-for="(item, index) in activeData.options" :key="index" class="select-item">
                <div class="select-line-icon option-drag">
                  <i class="el-icon-s-operation" />
                </div>
                <el-input v-model="item.label" placeholder="Option Name" size="small" />
                <el-input
                  placeholder="Option Value"
                  size="small"
                  :value="item.value"
                  @input="setOptionValue(item, $event)"
                />
                <div class="close-btn select-line-icon" @click="activeData.options.splice(index, 1)">
                  <i class="el-icon-remove-outline" />
                </div>
              </div>
            </draggable>
            <div style="margin-left: 20px;">
              <el-button
                style="padding-bottom: 0"
                icon="el-icon-circle-plus-outline"
                type="text"
                @click="addSelectItem"
              >
              Add Option
              </el-button>
            </div>
            <el-divider />
          </template>

          <template v-if="['el-cascader'].indexOf(activeData.tag) > -1">
            <el-divider>Options</el-divider>
            <el-form-item label="Data Type">
              <el-radio-group v-model="activeData.dataType" size="small">
                <el-radio-button label="dynamic">
                  Dynamic Data
                </el-radio-button>
                <el-radio-button label="static">
                  Static Data
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <template v-if="activeData.dataType === 'dynamic'">
              <el-form-item label="Label Key">
                <el-input v-model="activeData.labelKey" placeholder="Please enter label key" />
              </el-form-item>
              <el-form-item label="Value Key">
                <el-input v-model="activeData.valueKey" placeholder="Please enter value key" />
              </el-form-item>
              <el-form-item label="Children Key">
                <el-input v-model="activeData.childrenKey" placeholder="Please enter children key" />
              </el-form-item>
            </template>

            <el-tree
              v-if="activeData.dataType === 'static'"
              draggable
              :data="activeData.options"
              node-key="id"
              :expand-on-click-node="false"
              :render-content="renderContent"
            />
            <div v-if="activeData.dataType === 'static'" style="margin-left: 20px">
              <el-button
                style="padding-bottom: 0"
                icon="el-icon-circle-plus-outline"
                type="text"
                @click="addTreeItem"
              >
              Add Parent
              </el-button>
            </div>
            <el-divider />
          </template>

          <el-form-item v-if="activeData.optionType !== undefined" label="Option Style">
            <el-radio-group v-model="activeData.optionType">
              <el-radio-button label="default">
                Default
              </el-radio-button>
              <el-radio-button label="button">
                Button
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData['active-color'] !== undefined" label="Active Color">
            <el-color-picker v-model="activeData['active-color']" />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-color'] !== undefined" label="Inactive Color">
            <el-color-picker v-model="activeData['inactive-color']" />
          </el-form-item>

          <el-form-item v-if="activeData['allow-half'] !== undefined" label="Allow Half Select">
            <el-switch v-model="activeData['allow-half']" />
          </el-form-item>
          <el-form-item v-if="activeData['show-text'] !== undefined" label="Show Text">
            <el-switch v-model="activeData['show-text']" @change="rateTextChange" />
          </el-form-item>
          <el-form-item v-if="activeData['show-score'] !== undefined" label="Show Score">
            <el-switch v-model="activeData['show-score']" @change="rateScoreChange" />
          </el-form-item>
          <el-form-item v-if="activeData['show-stops'] !== undefined" label="Show Stops">
            <el-switch v-model="activeData['show-stops']" />
          </el-form-item>
          <el-form-item v-if="activeData.range !== undefined" label="Range Select">
            <el-switch v-model="activeData.range" @change="rangeChange" />
          </el-form-item>
          <el-form-item
            v-if="activeData.border !== undefined && activeData.optionType === 'default'"
            label="With Border"
          >
            <el-switch v-model="activeData.border" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-color-picker'" label="Color Format">
            <el-select
              v-model="activeData['color-format']"
              placeholder="Select color format"
              :style="{ width: '100%' }"
              @change="colorFormatChange"
            >
              <el-option
                v-for="(item, index) in colorFormatOptions"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            v-if="activeData.size !== undefined &&
              (activeData.optionType === 'button' ||
                activeData.border ||
                activeData.tag === 'el-color-picker')"
            label="Option Size"
          >
            <el-radio-group v-model="activeData.size">
              <el-radio-button label="medium">
                Medium
              </el-radio-button>
              <el-radio-button label="small">
                Small
              </el-radio-button>
              <el-radio-button label="mini">
                Mini
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData['show-word-limit'] !== undefined" label="Input Statistics">
            <el-switch v-model="activeData['show-word-limit']" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="Strict Step">
            <el-switch v-model="activeData['step-strictly']" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="Multiple Selection">
            <el-switch v-model="activeData.props.props.multiple" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="Show Full Path">
            <el-switch v-model="activeData['show-all-levels']" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="Filterable">
            <el-switch v-model="activeData.filterable" />
          </el-form-item>
          <el-form-item v-if="activeData.clearable !== undefined" label="Clearable">
            <el-switch v-model="activeData.clearable" />
          </el-form-item>
          <el-form-item v-if="activeData.showTip !== undefined" label="Show Tooltip">
            <el-switch v-model="activeData.showTip" />
          </el-form-item>
          <el-form-item v-if="activeData.multiple !== undefined" label="Multiple File Selection">
            <el-switch v-model="activeData.multiple" />
          </el-form-item>
          <el-form-item v-if="activeData['auto-upload'] !== undefined" label="Auto Upload">
            <el-switch v-model="activeData['auto-upload']" />
          </el-form-item>
          <el-form-item v-if="activeData.readonly !== undefined" label="Read Only">
            <el-switch v-model="activeData.readonly" />
          </el-form-item>
          <el-form-item v-if="activeData.disabled !== undefined" label="Disabled">
            <el-switch v-model="activeData.disabled" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-select'" label="Seachable">
            <el-switch v-model="activeData.filterable" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-select'" label="Multiple Selection">
            <el-switch v-model="activeData.multiple" @change="multipleChange" />
          </el-form-item>
          <el-form-item v-if="activeData.required !== undefined" label="Required">
            <el-switch v-model="activeData.required" />
          </el-form-item>

          <template v-if="activeData.layoutTree">
            <el-divider>Layout Structure Tree</el-divider>
            <el-tree
              :data="[activeData]"
              :props="layoutTreeProps"
              node-key="renderKey"
              default-expand-all
              draggable
            >
              <span slot-scope="{ node, data }">
                <span class="node-label">
                  <svg-icon class="node-icon" :icon-class="data.tagIcon" />
                  {{ node.label }}
                </span>
              </span>
            </el-tree>
          </template>

          <template v-if="activeData.layout === 'colFormItem'">
            <el-divider>Regex Validation</el-divider>
            <div
              v-for="(item, index) in activeData.regList"
              :key="index"
              class="reg-item"
            >
              <span class="close-btn" @click="activeData.regList.splice(index, 1)">
                <i class="el-icon-close" />
              </span>
              <el-form-item label="Expression">
                <el-input v-model="item.pattern" placeholder="Please enter regex" />
              </el-form-item>
              <el-form-item label="Error Message" style="margin-bottom:0">
                <el-input v-model="item.message" placeholder="Please enter error message" />
              </el-form-item>
            </div>
            <div style="margin-left: 20px">
              <el-button icon="el-icon-circle-plus-outline" type="text" @click="addReg">
                Add Rule
              </el-button>
            </div>
          </template>
        </el-form>
        <!-- 表单属性 -->
        <el-form v-show="currentTab === 'form'" size="small" label-width="90px">
          <el-form-item label="Form Name">
            <el-input v-model="formConf.formRef" placeholder="Please enter form name (ref)" />
          </el-form-item>
          <el-form-item label="Form Model">
            <el-input v-model="formConf.formModel" placeholder="Please enter data model" />
          </el-form-item>
          <el-form-item label="Validation Model">
            <el-input v-model="formConf.formRules" placeholder="Please enter validation model" />
          </el-form-item>
          <el-form-item label="Form Size">
            <el-radio-group v-model="formConf.size">
              <el-radio-button label="medium">
                Medium
              </el-radio-button>
              <el-radio-button label="small">
                Small
              </el-radio-button>
              <el-radio-button label="mini">
                Mini
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Label Alignment">
            <el-radio-group v-model="formConf.labelPosition">
              <el-radio-button label="left">
                Left Align
              </el-radio-button>
              <el-radio-button label="right">
                Right Align
              </el-radio-button>
              <el-radio-button label="top">
                Top Align
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Label Width">
            <el-input-number v-model="formConf.labelWidth" placeholder="Label Width" />
          </el-form-item>
          <el-form-item label="Grid Spacing">
            <el-input-number v-model="formConf.gutter" :min="0" placeholder="Grid Spacing" />
          </el-form-item>
          <el-form-item label="Disable Form">
            <el-switch v-model="formConf.disabled" />
          </el-form-item>
          <el-form-item label="Form Buttons">
            <el-switch v-model="formConf.formBtns" />
          </el-form-item>
          <el-form-item label="Show Border for Unfocused Components">
            <el-switch v-model="formConf.unFocusedComponentBorder" />
          </el-form-item>
        </el-form>
      </el-scrollbar>
    </div>

    <treeNode-dialog :visible.sync="dialogVisible" title="Add Option" @commit="addNode" />
    <icons-dialog :visible.sync="iconsVisible" :current="activeData[currentIconModel]" @select="setIcon" />
  </div>
</template>

<script>
import { isArray } from 'util'
import draggable from 'vuedraggable'
import TreeNodeDialog from './TreeNodeDialog'
import { isNumberStr } from '@/utils/index'
import IconsDialog from './IconsDialog'
import {
  inputComponents,
  selectComponents,
  layoutComponents
} from '@/utils/generator/config'

const dateTimeFormat = {
  date: 'yyyy-MM-dd',
  week: 'yyyy Week WW',
  month: 'yyyy-MM',
  year: 'yyyy',
  datetime: 'yyyy-MM-dd HH:mm:ss',
  daterange: 'yyyy-MM-dd',
  monthrange: 'yyyy-MM',
  datetimerange: 'yyyy-MM-dd HH:mm:ss'
}

export default {
  components: {
    draggable,
    TreeNodeDialog,
    IconsDialog
  },
  props: ['showField', 'activeData', 'formConf'],
  data() {
    return {
      currentTab: 'field',
      currentNode: null,
      dialogVisible: false,
      iconsVisible: false,
      currentIconModel: null,
      dateTypeOptions: [
        {
          label: 'Day (date)',
          value: 'date'
        },
        {
          label: 'Week (week)',
          value: 'week'
        },
        {
          label: 'Month (month)',
          value: 'month'
        },
        {
          label: 'Year (year)',
          value: 'year'
        },
        {
          label: 'Date Time (datetime)',
          value: 'datetime'
        }
      ],
      dateRangeTypeOptions: [
        {
          label: 'Date Range (daterange)',
          value: 'daterange'
        },
        {
          label: 'Month Range (monthrange)',
          value: 'monthrange'
        },
        {
          label: 'Date Time Range (datetimerange)',
          value: 'datetimerange'
        }
      ],
      colorFormatOptions: [
        {
          label: 'hex',
          value: 'hex'
        },
        {
          label: 'rgb',
          value: 'rgb'
        },
        {
          label: 'rgba',
          value: 'rgba'
        },
        {
          label: 'hsv',
          value: 'hsv'
        },
        {
          label: 'hsl',
          value: 'hsl'
        }
      ],
      justifyOptions: [
        {
          label: 'start',
          value: 'start'
        },
        {
          label: 'end',
          value: 'end'
        },
        {
          label: 'center',
          value: 'center'
        },
        {
          label: 'space-around',
          value: 'space-around'
        },
        {
          label: 'space-between',
          value: 'space-between'
        }
      ],
      layoutTreeProps: {
        label(data, node) {
          return data.componentName || `${data.label}: ${data.vModel}`
        }
      }
    }
  },
  computed: {
    documentLink() {
      return (
        this.activeData.document
        || 'https://element.eleme.cn/#/en-US/component/installation'
      )
    },
    dateOptions() {
      if (
        this.activeData.type !== undefined
        && this.activeData.tag === 'el-date-picker'
      ) {
        if (this.activeData['start-placeholder'] === undefined) {
          return this.dateTypeOptions
        }
        return this.dateRangeTypeOptions
      }
      return []
    },
    tagList() {
      return [
        {
          label: 'Input Components',
          options: inputComponents
        },
        {
          label: 'Selection Components',
          options: selectComponents
        }
      ]
    }
  },
  methods: {
    addReg() {
      this.activeData.regList.push({
        pattern: '',
        message: ''
      })
    },
    addSelectItem() {
      this.activeData.options.push({
        label: '',
        value: ''
      })
    },
    addTreeItem() {
      ++this.idGlobal
      this.dialogVisible = true
      this.currentNode = this.activeData.options
    },
    renderContent(h, { node, data, store }) {
      return (
        <div class="custom-tree-node">
          <span>{node.label}</span>
          <span class="node-operation">
            <i on-click={() => this.append(data)}
              class="el-icon-plus"
              title="Add"
            ></i>
            <i on-click={() => this.remove(node, data)}
              class="el-icon-delete"
              title="Delete"
            ></i>
          </span>
        </div>
      )
    },
    append(data) {
      if (!data.children) {
        this.$set(data, 'children', [])
      }
      this.dialogVisible = true
      this.currentNode = data.children
    },
    remove(node, data) {
      const { parent } = node
      const children = parent.data.children || parent.data
      const index = children.findIndex(d => d.id === data.id)
      children.splice(index, 1)
    },
    addNode(data) {
      this.currentNode.push(data)
    },
    setOptionValue(item, val) {
      item.value = isNumberStr(val) ? +val : val
    },
    setDefaultValue(val) {
      if (Array.isArray(val)) {
        return val.join(',')
      }
      if (['string', 'number'].indexOf(val) > -1) {
        return val
      }
      if (typeof val === 'boolean') {
        return `${val}`
      }
      return val
    },
    onDefaultValueInput(str) {
      if (isArray(this.activeData.defaultValue)) {
        // 数组
        this.$set(
          this.activeData,
          'defaultValue',
          str.split(',').map(val => (isNumberStr(val) ? +val : val))
        )
      } else if (['true', 'false'].indexOf(str) > -1) {
        // 布尔
        this.$set(this.activeData, 'defaultValue', JSON.parse(str))
      } else {
        // 字符串和数字
        this.$set(
          this.activeData,
          'defaultValue',
          isNumberStr(str) ? +str : str
        )
      }
    },
    onSwitchValueInput(val, name) {
      if (['true', 'false'].indexOf(val) > -1) {
        this.$set(this.activeData, name, JSON.parse(val))
      } else {
        this.$set(this.activeData, name, isNumberStr(val) ? +val : val)
      }
    },
    setTimeValue(val, type) {
      const valueFormat = type === 'week' ? dateTimeFormat.date : val
      this.$set(this.activeData, 'defaultValue', null)
      this.$set(this.activeData, 'value-format', valueFormat)
      this.$set(this.activeData, 'format', val)
    },
    spanChange(val) {
      this.formConf.span = val
    },
    multipleChange(val) {
      this.$set(this.activeData, 'defaultValue', val ? [] : '')
    },
    dateTypeChange(val) {
      this.setTimeValue(dateTimeFormat[val], val)
    },
    rangeChange(val) {
      this.$set(
        this.activeData,
        'defaultValue',
        val ? [this.activeData.min, this.activeData.max] : this.activeData.min
      )
    },
    rateTextChange(val) {
      if (val) this.activeData['show-score'] = false
    },
    rateScoreChange(val) {
      if (val) this.activeData['show-text'] = false
    },
    colorFormatChange(val) {
      this.activeData.defaultValue = null
      this.activeData['show-alpha'] = val.indexOf('a') > -1
      this.activeData.renderKey = +new Date() // 更新renderKey,重新渲染该组件
    },
    openIconsDialog(model) {
      this.iconsVisible = true
      this.currentIconModel = model
    },
    setIcon(val) {
      this.activeData[this.currentIconModel] = val
    },
    tagChange(tagIcon) {
      let target = inputComponents.find(item => item.tagIcon === tagIcon)
      if (!target) target = selectComponents.find(item => item.tagIcon === tagIcon)
      this.$emit('tag-change', target)
    }
  }
}
</script>

<style lang="scss" scoped>
.right-board {
  width: 350px;
  position: absolute;
  right: 0;
  top: 0;
  padding-top: 3px;
  .field-box {
    position: relative;
    height: calc(100vh - 42px);
    box-sizing: border-box;
    overflow: hidden;
  }
  .el-scrollbar {
    height: 100%;
  }
}
.select-item {
  display: flex;
  border: 1px dashed #fff;
  box-sizing: border-box;
  & .close-btn {
    cursor: pointer;
    color: #f56c6c;
  }
  & .el-input + .el-input {
    margin-left: 4px;
  }
}
.select-item + .select-item {
  margin-top: 4px;
}
.select-item.sortable-chosen {
  border: 1px dashed #409eff;
}
.select-line-icon {
  line-height: 32px;
  font-size: 22px;
  padding: 0 4px;
  color: #777;
}
.option-drag {
  cursor: move;
}
.time-range {
  .el-date-editor {
    width: 227px;
  }
  ::v-deep .el-icon-time {
    display: none;
  }
}
.document-link {
  position: absolute;
  display: block;
  width: 26px;
  height: 26px;
  top: 0;
  left: 0;
  cursor: pointer;
  background: #409eff;
  z-index: 1;
  border-radius: 0 0 6px 0;
  text-align: center;
  line-height: 26px;
  color: #fff;
  font-size: 18px;
}
.node-label{
  font-size: 14px;
}
.node-icon{
  color: #bebfc3;
}
</style>
