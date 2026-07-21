import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, InputNumber, Space, message, Select, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

interface Sensor {
  id: string;
  sensorCode: string;
  name: string;
  model: string;
  minValue: number;
  maxValue: number;
  status: string;
}

export default function SensorList() {
  const [sensors, setSensors] = useState<Sensor[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form] = Form.useForm();

  // Khai báo các biến phục vụ lọc động
  const [searchName, setSearchName] = useState<string>('');
  const [searchStatus, setSearchStatus] = useState<string>('');

  const fetchSensors = async () => {
    setLoading(true);
    try {
      const response = await axiosClient.post('/sensors/filter', {
        name: searchName,
        status: searchStatus || null,
        page: 0,
        size: 20,
        sortBy: 'createdDate',
        sortDir: 'DESC'
      });
      setSensors(response.data.data.content);
    } catch (error) {
      message.error("Không thể tải danh sách cảm biến!");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSensors();
  }, [searchStatus]);

  const handleSave = async (values: any) => {
    try {
      if (editingId) {
        await axiosClient.put('/sensors', { id: editingId, ...values });
        message.success("Cập nhật cảm biến thành công!");
      } else {
        await axiosClient.post('/sensors', values);
        message.success("Thêm mới cảm biến thành công!");
      }
      setIsModalOpen(false);
      setEditingId(null);
      form.resetFields();
      fetchSensors();
    } catch (error) {
      message.error("Xử lý thất bại!");
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await axiosClient.delete(`/sensors/${id}`);
      message.success("Đã xóa cảm biến thành công!");
      fetchSensors();
    } catch (error) {
      message.error("Xóa cảm biến thất bại!");
    }
  };

  const openEditModal = (sensor: Sensor) => {
    setEditingId(sensor.id);
    form.setFieldsValue(sensor);
    setIsModalOpen(true);
  };

  const columns = [
    { title: 'Mã Cảm Biến', dataIndex: 'sensorCode', key: 'sensorCode' },
    { title: 'Tên Cảm Biến', dataIndex: 'name', key: 'name' },
    { title: 'Dải Đo (Min)', dataIndex: 'minValue', key: 'minValue', render: (val: number) => <Tag color="blue">{val}</Tag> },
    { title: 'Dải Đo (Max)', dataIndex: 'maxValue', key: 'maxValue', render: (val: number) => <Tag color="red">{val}</Tag> },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (status: string) => <Tag color={status === 'ACTIVE' ? 'green' : 'orange'}>{status}</Tag> },
    {
      title: 'Hành động',
      key: 'action',
      render: (_: any, record: Sensor) => (
        <Space size="middle">
          <Button type="link" icon={<EditOutlined />} onClick={() => openEditModal(record)}>Sửa</Button>
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>Xóa</Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <h3 className="text-xl font-bold mb-6 text-gray-800">Quản lý thiết bị cảm biến</h3>
      
      {/* Thanh lọc động */}
      <Space style={{ marginBottom: 16 }} wrap>
        <Input 
          placeholder="Tìm theo tên cảm biến" 
          value={searchName} 
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchName(e.target.value)} 
          style={{ width: 200 }} 
        />
        <Select
          placeholder="Chọn trạng thái"
          onChange={(value: string | null) => setSearchStatus(value || '')}
          allowClear
          style={{ width: 150 }}
        >
          <Select.Option value="ACTIVE">ACTIVE</Select.Option>
          <Select.Option value="INACTIVE">INACTIVE</Select.Option>
        </Select>
        <Button type="primary" icon={<SearchOutlined />} onClick={fetchSensors}>Tìm kiếm</Button>
        <Button 
          type="primary" 
          icon={<PlusOutlined />} 
          onClick={() => { setEditingId(null); form.resetFields(); setIsModalOpen(true); }}
          style={{ background: '#52c41a' }}
        >
          Đăng Ký Cảm Biến mới
        </Button>
      </Space>

      {/* Hiển thị bảng dữ liệu */}
      <Table dataSource={sensors} columns={columns} rowKey="id" loading={loading} />

      <Modal 
        title={editingId ? "Cập nhật cấu hình cảm biến" : "Đăng ký cảm biến mới"} 
        open={isModalOpen} 
        onOk={() => form.submit()} 
        onCancel={() => { setIsModalOpen(false); setEditingId(null); }}
      >
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item name="id" label="Mã phần cứng (MAC Address / UUID)" rules={[{ required: true }]}>
            <Input disabled={editingId !== null} placeholder="Ví dụ: sensor_mac_01" />
          </Form.Item>
          <Form.Item name="sensorCode" label="Mã sensor định danh" rules={[{ required: true }]}>
            <Input placeholder="Ví dụ: SS-TEMP-01" />
          </Form.Item>
          <Form.Item name="name" label="Tên cảm biến" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="gatewayId" label="Mã Gateway liên kết" rules={[{ required: true }]}>
            <Input placeholder="Dán mã UUID của Gateway liên kết vào đây" />
          </Form.Item>
          <Form.Item name="sensorTypeId" label="Mã loại cảm biến" rules={[{ required: true }]}>
            <Input placeholder="Dán mã UUID của Loại cảm biến vào đây" />
          </Form.Item>
          <Form.Item name="minValue" label="Ngưỡng cảnh báo dưới (Min)">
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="maxValue" label="Ngưỡng cảnh báo trên (Max)">
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}