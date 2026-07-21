import { Card, Col, Row, Statistic } from 'antd';
import { ArrowUpOutlined, CheckCircleOutlined, AlertOutlined } from '@ant-design/icons';

export default function Dashboard() {
  return (
    <div>
      <h2 className="text-2xl font-bold mb-6 text-gray-800">Bảng điều khiển hệ thống thời gian thực</h2>
      
      <Row gutter={16} className="mb-8">
        <Col span={8}>
          <Card bordered={false} className="shadow-sm">
            <Statistic
              title="Tổng số Trạm hoạt động"
              value={5}
              valueStyle={{ color: '#3f8600' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} className="shadow-sm">
            <Statistic
              title="Tổng số Cảm biến"
              value={9}
              valueStyle={{ color: '#3f8600' }}
              prefix={<ArrowUpOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} className="shadow-sm">
            <Statistic
              title="Sự cố cảnh báo (24h)"
              value={0}
              valueStyle={{ color: '#cf1322' }}
              prefix={<AlertOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <div className="bg-white p-4 border rounded-2xl shadow-sm">
        <h3 className="text-lg font-bold mb-4 text-gray-700">Biểu đồ giám sát nhiệt độ và độ ẩm thực tế</h3>
        <iframe
          src="http://localhost:3000/d-solo/your-dashboard-uid/telemetry-monitor?orgId=1&panelId=1&refresh=5s"
          width="100%"
          height="450"
          frameBorder="0"
          className="rounded-xl border border-gray-100"
        ></iframe>
      </div>
    </div>
  );
}