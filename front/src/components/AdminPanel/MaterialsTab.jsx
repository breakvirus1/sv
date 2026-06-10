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

const MaterialsTab = ({ materialsData, onAddClick, onEditClick, onDeleteClick }) => {
  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление материалами</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={onAddClick}>
          Добавить материал
        </Button>
      </Box>
      {materialsData.length === 0 && <Typography>Нет данных</Typography>}
      {materialsData.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Название</TableCell>
                <TableCell>Ед. изм.</TableCell>
                <TableCell>Цена</TableCell>
                <TableCell>Коэф. отход</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {materialsData.map((mat) => (
                <TableRow key={mat.id}>
                  <TableCell>{mat.name}</TableCell>
                  <TableCell>{mat.unit}</TableCell>
                  <TableCell>{mat.price?.toFixed(2)} ₽</TableCell>
                  <TableCell>{mat.wasteCoefficient?.toString()}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => onEditClick(mat)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => onDeleteClick(mat)}><Delete /></IconButton>
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

export default MaterialsTab;
