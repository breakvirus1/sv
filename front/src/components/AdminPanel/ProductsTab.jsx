import {
  Box,
  Typography,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Button,
  IconButton,
  TableContainer,
  Paper
} from '@mui/material';
import { Add, Edit, Delete } from '@mui/icons-material';

const ProductsTab = ({ productsData, onAddClick, onEditClick, onDeleteClick }) => {
  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление продуктами (шаблоны изделий)</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={onAddClick}>
          Добавить продукт
        </Button>
      </Box>
      {productsData.length === 0 && <Typography>Нет данных</Typography>}
      {productsData.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Название</TableCell>
                <TableCell>Артикул</TableCell>
                <TableCell>Базовая цена</TableCell>
                <TableCell>Материалов</TableCell>
                <TableCell>Операций</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {productsData.map((prod) => (
                <TableRow key={prod.id}>
                  <TableCell>{prod.name}</TableCell>
                  <TableCell>{prod.article || '-'}</TableCell>
                  <TableCell>{(prod.basePrice || 0).toFixed(2)} ₽</TableCell>
                  <TableCell>{prod.materials ? prod.materials.length : 0}</TableCell>
                  <TableCell>{prod.operations ? prod.operations.length : 0}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => onEditClick(prod)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => onDeleteClick(prod)}><Delete /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
};

export default ProductsTab;
