package br.com.yson.controle.de.obras.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- FORNECEDORES ---
    @Query("SELECT * FROM fornecedores ORDER BY nome ASC")
    fun getAllFornecedores(): Flow<List<Fornecedor>>

    @Query("SELECT * FROM fornecedores WHERE id = :id")
    suspend fun getFornecedorById(id: Int): Fornecedor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFornecedor(fornecedor: Fornecedor): Long

    @Update
    suspend fun updateFornecedor(fornecedor: Fornecedor)

    @Delete
    suspend fun deleteFornecedor(fornecedor: Fornecedor)


    // --- PRESTADORES ---
    @Query("SELECT * FROM prestadores ORDER BY nome ASC")
    fun getAllPrestadores(): Flow<List<Prestador>>

    @Query("SELECT * FROM prestadores WHERE id = :id")
    suspend fun getPrestadorById(id: Int): Prestador?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrestador(prestador: Prestador): Long

    @Update
    suspend fun updatePrestador(prestador: Prestador)

    @Delete
    suspend fun deletePrestador(prestador: Prestador)


    // --- OBRAS ---
    @Query("SELECT * FROM obras ORDER BY nome ASC")
    fun getAllObras(): Flow<List<Obra>>

    @Query("SELECT * FROM obras WHERE id = :id")
    suspend fun getObraById(id: Int): Obra?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObra(obra: Obra): Long

    @Delete
    suspend fun deleteObra(obra: Obra)


    // --- FORNECEDORES DA OBRA ---
    @Query("""
        SELECT 
            fo.id as id,
            fo.obraId as obraId,
            fo.fornecedorId as fornecedorId,
            fo.valorServico as valorServico,
            fo.valorPago as valorPago,
            fo.dataPagamento as dataPagamento,
            f.nome as fornecedorNome,
            f.telefone as fornecedorTelefone
        FROM fornecedores_obra fo
        INNER JOIN fornecedores f ON fo.fornecedorId = f.id
        WHERE fo.obraId = :obraId
    """)
    fun getFornecedoresForObra(obraId: Int): Flow<List<FornecedorObraWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFornecedorObra(fornecedorObra: FornecedorObra): Long

    @Delete
    suspend fun deleteFornecedorObra(fornecedorObra: FornecedorObra)


    // --- PAGAMENTOS FORNECEDOR ---
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0.0) 
        FROM pagamentos_fornecedor p
        INNER JOIN fornecedores_obra fo ON p.fornecedorObraId = fo.id
        WHERE fo.fornecedorId = :fornecedorId
    """)
    suspend fun getTotalPaidForFornecedor(fornecedorId: Int): Double

    @Query("SELECT * FROM pagamentos_fornecedor WHERE fornecedorObraId = :fornecedorObraId ORDER BY id DESC")
    fun getPagamentosForFornecedorObra(fornecedorObraId: Int): Flow<List<PagamentoFornecedor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagamentoFornecedor(pagamento: PagamentoFornecedor): Long

    @Delete
    suspend fun deletePagamentoFornecedor(pagamento: PagamentoFornecedor)


    // --- PRESTADORES DA OBRA ---
    @Query("""
        SELECT 
            po.id as id,
            po.obraId as obraId,
            po.prestadorId as prestadorId,
            po.valorMaterial as valorMaterial,
            po.valorPago as valorPago,
            po.dataPagamento as dataPagamento,
            p.nome as prestadorNome,
            p.telefone as prestadorTelefone
        FROM prestadores_obra po
        INNER JOIN prestadores p ON po.prestadorId = p.id
        WHERE po.obraId = :obraId
    """)
    fun getPrestadoresForObra(obraId: Int): Flow<List<PrestadorObraWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrestadorObra(prestadorObra: PrestadorObra): Long

    @Delete
    suspend fun deletePrestadorObra(prestadorObra: PrestadorObra)


    // --- PAGAMENTOS PRESTADOR ---
    @Query("SELECT * FROM pagamentos_prestador WHERE prestadorObraId = :prestadorObraId ORDER BY id DESC")
    fun getPagamentosForPrestadorObra(prestadorObraId: Int): Flow<List<PagamentoPrestador>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagamentoPrestador(pagamento: PagamentoPrestador): Long

    @Delete
    suspend fun deletePagamentoPrestador(pagamento: PagamentoPrestador)

    // --- LOTES DA OBRA ---
    @Query("SELECT * FROM lotes_obra WHERE obraId = :obraId")
    fun getLotesForObra(obraId: Int): Flow<List<LoteObra>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoteObra(loteObra: LoteObra): Long

    @Delete
    suspend fun deleteLoteObra(loteObra: LoteObra)

    // --- BUROCRACIA DA OBRA ---
    @Query("SELECT * FROM burocracia_obra WHERE obraId = :obraId ORDER BY nome ASC")
    fun getBurocraciasForObra(obraId: Int): Flow<List<BurocraciaObra>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBurocraciaObra(burocracia: BurocraciaObra): Long

    @Delete
    suspend fun deleteBurocraciaObra(burocracia: BurocraciaObra)
}
