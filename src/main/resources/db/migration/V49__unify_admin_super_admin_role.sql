UPDATE users
SET admin_role = 'ADMIN'
WHERE role = 'ADMIN'
  AND admin_role = 'SUPER_ADMIN';
