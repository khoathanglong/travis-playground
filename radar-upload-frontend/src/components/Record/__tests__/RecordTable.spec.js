/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import { Store } from 'vuex-mock-store';
import RecordTable from '../RecordTable.vue';
import fileAPI from '@/axios/file';

describe('RecordTable', () => {
  // call this api when component is created
  const projectID = '1111';
  const $store = new Store({
    state: {
      project: {
        currentProject: {
          value: projectID,
        },
      },
      file: {
        searchText: 'search text',
      },
    },
  });
  const wrapper = shallowMount(RecordTable, {
    propsData: {
      isActive: false,
      currentProject: projectID,
    },
    mocks: {
      $store,
    },
    filter: {
      moment: () => jest.fn(),
    },
    stubs: ['v-data-table'],
  });

  it('has isActive props', () => {
    expect(wrapper.vm.isActive).toBe(false);
    expect(wrapper.vm.currentProject).toBe(projectID);
  });

  it('call api to get file list if it is active tab and a project is selected', async () => {
    const resolvedValue = [{ name: 'name' }];
    fileAPI.filterRecords = jest.fn().mockResolvedValue(resolvedValue);
    wrapper.setProps({ isActive: true });
    wrapper.vm.$store.state.project.currentProject.value = 'watchingProject';

    expect(wrapper.vm.loading).toBe(true);
    await flushPromises();
    expect(fileAPI.filterRecords).toBeCalledWith({ projectId: 'watchingProject', getRecordOnly: true });
    expect(wrapper.vm.recordList).toEqual(resolvedValue);
    expect(wrapper.vm.loading).toBe(false);
  });

  it('watch isActive = true', () => {
    const getRecordList = jest.spyOn(wrapper.vm, 'getRecordList');
    wrapper.setProps({ isActive: false });
    wrapper.setProps({ isActive: true });
    expect(getRecordList).toBeCalledWith(wrapper.vm.currentProject);
  });

  it('getRecordList: CASE ERROR', async () => {
    const projectId = 'project id';
    fileAPI.filterRecords = jest.fn().mockRejectedValue('rejectedValue');
    wrapper.vm.getRecordList(projectId);
    expect(wrapper.vm.loading).toBe(true);
    expect(wrapper.vm.recordList).toEqual([]);
    await flushPromises();
    expect(wrapper.vm.recordList).toEqual([]);
    expect(wrapper.vm.loading).toBe(false);
  });
});