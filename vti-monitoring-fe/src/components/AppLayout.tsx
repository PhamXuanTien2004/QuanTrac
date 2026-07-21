import { useState } from 'react';
import { Layout, Menu, Button } from 'antd';
import { DashboardOutlined, EnvironmentOutlined, PartitionOutlined, LogoutOutlined } from '@ant-design/icons';
import Dashboard from '../pages/Dashboard';
import StationList from '../pages/StationList';
import SensorList from '../pages/SensorList';
import keycloak from '../keycloak';

const { Header, Content, Sider } = Layout;

export interface MenuInfo {
  key: string;
  keyPath: string[];
}

export default function AppLayout() {
  const [activeTab, setActiveTab] = useState<string>('dashboard');

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard': return <Dashboard />;
      case 'stations': return <StationList />;
      case 'sensors': return <SensorList />;
      default: return <Dashboard />;
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" collapsible>
        <div className="text-white text-center py-4 font-bold text-lg bg-gray-900">
          VTI MONITORING
        </div>
        <Menu 
          theme="dark" 
          defaultSelectedKeys={['dashboard']} 
          mode="inline"
          // Định nghĩa rõ ràng kiểu dữ liệu MenuInfo để tránh lỗi implicit any
          onClick={(info: MenuInfo) => setActiveTab(info.key)}
        >
          <Menu.Item key="dashboard" icon={<DashboardOutlined />}>Dashboard</Menu.Item>
          <Menu.Item key="stations" icon={<EnvironmentOutlined />}>Quản lý Trạm</Menu.Item>
          <Menu.Item key="sensors" icon={<PartitionOutlined />}>Quản lý Cảm biến</Menu.Item>
        </Menu>
      </Sider>
      <Layout>
        <Header className="bg-white px-6 flex justify-between items-center shadow-sm" style={{ background: '#fff' }}>
          <span className="font-semibold text-gray-700">Chào mừng, {keycloak.tokenParsed?.preferred_username}</span>
          <Button 
            type="primary" 
            danger 
            icon={<LogoutOutlined />} 
            onClick={() => keycloak.logout()}
          >
            Đăng xuất
          </Button>
        </Header>
        <Content style={{ margin: '16px' }}>
          <div style={{ padding: 24, background: '#fff', minHeight: 360 }}>
            {renderContent()}
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}