import React from 'react';

const OperationsSelector = ({ operations, selectedOperations, onSelect, loading }) => {
  const handleToggle = (operation) => {
    const isSelected = selectedOperations.some(op => op.id === operation.id);
    if (isSelected) {
      onSelect(selectedOperations.filter(op => op.id !== operation.id));
    } else {
      onSelect([...selectedOperations, operation]);
    }
  };

  return (
    <div className="mb-4">
      <label className="block text-sm font-medium mb-2">Select Operations</label>
      {loading ? (
        <p>Loading operations...</p>
      ) : (
        <div className="space-y-2">
          {operations.map(operation => (
            <div key={operation.id} className="flex items-center">
              <input
                type="checkbox"
                checked={selectedOperations.some(op => op.id === operation.id)}
                onChange={() => handleToggle(operation)}
                className="mr-2"
                disabled={loading}
              />
              <span>{operation.name} - {operation.price} руб/{operation.unit}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default OperationsSelector;
