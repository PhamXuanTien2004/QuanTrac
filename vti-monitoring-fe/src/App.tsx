import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './features/auth/pages/LoginPage';
import { RegisterPage } from './features/auth/pages/RegisterPage';

function App() {
  return (
    <Router>
      <Routes>
        {/* Mặc định vào ứng dụng sẽ chuyển hướng thẳng đến trang Login */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        
        {/* Khai báo các Route cho tính năng Auth */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Routes>
    </Router>
  );
}

export default App;