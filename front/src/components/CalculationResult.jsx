import React from 'react';

const CalculationResult = ({ result }) => {
  return (
    <div className="mt-4 p-4 border rounded">
      <h2 className="text-xl font-semibold mb-2">Calculation Result</h2>
      <p><strong>Material:</strong> {result.materialName}</p>
      <p><strong>Dimensions:</strong> {result.widthM} x {result.heightM} m</p>
      <p><strong>Total Price:</strong> {result.totalPrice} руб</p>
      <h3 className="text-lg font-medium mt-4">Operations:</h3>
      <ul>
        {result.operations.map((op, index) => (
          <li key={index}>
            {op.operationName}: {op.quantity} {op.unit} x {op.pricePerUnit} = {op.subtotal} руб
          </li>
        ))}
      </ul>
      <p><strong>Created At:</strong> {new Date(result.createdAt).toLocaleString()}</p>
    </div>
  );
};

export default CalculationResult;