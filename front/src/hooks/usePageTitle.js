import { useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuth } from '../context/AuthContext';

const routeTitles = {
  '/': 'PrintSV',
  '/login': 'Вход',
  '/callback': 'Авторизация',
  '/dashboard': 'Дашборд',
  '/orders': 'Заказы',
  '/orders/new': 'Новый заказ',
  '/orders/product': 'Заказ из конструктора',
  '/production': 'Производство',
  '/production/positions': 'Позиции производства',
  '/manager': 'Кабинет менеджера',
  '/manager/dashboard': 'Дашборд менеджера',
  '/manager/orders': 'Заказы менеджера',
  '/admin': 'Админ-панель',
  '/admin-dashboard': 'Админ-дашборд',
  '/constructor/new': 'Конструктор изделий',
  '/notifications': 'Уведомления',
  '/test-calculations': 'Тестовые расчёты',
};

export const usePageTitle = () => {
  const location = useLocation();
  const { user } = useAuth();

  useEffect(() => {
    let title = routeTitles[location.pathname];

    if (!title) {
      for (const [route, routeTitle] of Object.entries(routeTitles)) {
        if (route !== '/' && location.pathname.startsWith(route)) {
          title = routeTitle;
          break;
        }
      }
    }

    if (!title) {
      title = 'PrintSV';
    }

    const companyName = user?.companyName || 'PrintSV';
    document.title = `${companyName} — ${title}`;
  }, [location.pathname, user?.companyName]);
};