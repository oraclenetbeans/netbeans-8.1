      PROGRAM PARTY
          
      DO 10 I=1,22
      CURSOR(1,I) = CDATA(I)
      LCURSOR(I) = LCDATA(I)
      DO 10 J=2,10
   10 CURSOR(J,I) = 0

      IF (LCLRLIN .NE. 0) THEN
         CALL WRITCH (JADD,CLRLIN,LCLRLIN,TERM,*1700,*1700)
      ELSE
         CALL WRITCH (JADD,SPACES,72,TERM,*1700,*1700)
      END IF
      
      END