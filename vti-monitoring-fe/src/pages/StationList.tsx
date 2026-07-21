import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, InputNumber, Space, message, Select } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

interface Station {
  id: string;
  stationCode: string;
  name: string;
  location: string;
  latitude: number;
  longitude: number;
  status: string;
}

export default function StationList() {
  const [stations, setStations] = useState<Station[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form] = Form.useForm();
  
  const [searchName, setSearchName] = useState<string>('');
  const [searchStatus, setSearchStatus] = useState<string>('');

  const fetchStations = async () => {
    setLoading(true);
    try {
      const response = await axiosClient.post('/stations/filter', {
        name: searchName,
        status: searchStatus || null,
        page: 0,
        size: 20,
        sortBy: 'createdDate',
        sortDir: 'DESC'
      });
      setStations(response.data.data.content);
    } catch (error) {
      message.error("Không thể tải danh sách trạm!");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStations();
  }, [searchStatus]);

  const handleSave = async (values: any) => {
    try {
      if (editingId) {
        await axiosClient.put(`/stations/${editingId}`, { id: editingId, ...values });
        message.success("Cập nhật trạm thành công!");
      } else {
        await axiosClient.post('/stations', values);
        message.success("Thêm mới trạm thành công!");
      }
      setIsModalOpen(false);
      setEditingId(null);
      form.resetFields();
      fetchStations();
    } catch (error) {
      message.error("Xử lý thất bại!");
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await axiosClient.delete(`/stations/${id}`);
      message.success("Đã xóa trạm quan trắc!");
      fetchStations();
    } catch (error) {
      message.error("Xóa trạm thất bại!");
    }
  };

  const openEditModal = (station: Station) => {
    setEditingId(station.id);
    form.setFieldsValue(station);
    setIsOpen(true);
  };

  const setIsOpen = (open: boolean) => {
    setIsModalOpen(open);
  };

  const columns = [
    { title: 'Mã Trạm', dataIndex: 'stationCode', key: 'stationCode' },
    { title: 'Tên Trạm', dataIndex: 'name', key: 'name' },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status' },
    {
      title: 'Hành động',
      key: 'action',
      render: (_: any, record: Station) => (
        <Space size="middle">
          <Button type="link" icon={<EditOutlined />} onClick={() => openEditModal(record)}>Sửa</Button>
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>Xóa</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h3 className="text-xl font-bold mb-6 text-gray-800">Quản lý trạm quan trắc</h3>
      
      <Space style={{ marginBottom: 16 }} wrap>
        <Input 
          placeholder="Tìm theo tên trạm" 
          value={searchName} 
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchName(e.target.value)} 
          style={{ width: 200 }}
        />
        <Select
          placeholder="Chọn trạng thái"
          onChange={(value: string) => setSearchStatus(value || '')}
          allowClear
          style={{ width: 150 }}
        >
          <Select.Option value="ONLINE">ONLINE</Select.Option>
          <Select.Option value="OFFLINE">OFFLINE</Select.Option>
        </Select>
        <Button type="primary" icon={<SearchOutlined />} onClick={fetchStations}>Tìm kiếm</Button>
        <Button 
          type="primary" 
          icon={<PlusOutlined />} 
          onClick={() => { setEditingId(null); form.resetFields(); setIsOpen(true); }}
          style={{ background: '#52c41a' }}
        >
          Thêm Trạm Mới
        </Button>
      </Space>

      <Table dataSource={stations} columns={columns} rowKey="id" loading={loading} />

      <Modal 
        title={editingId ? "Cập nhật trạm quan trắc" : "Thêm trạm quan trắc mới"} 
        open={isModalOpen} 
        onOk={() => form.submit()} 
        onCancel={() => { setIsOpen(false); setEditingId(null); }}
      >
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item name="stationCode" label="Mã trạm" rules={[{ required: true }]}>
            <Input disabled={editingId !== null} />
          </Form.Item>
          <Form.Item name="name" label="Tên trạm" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="latitude" label="Vĩ độ" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="longitude" label="Kinh độ" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}